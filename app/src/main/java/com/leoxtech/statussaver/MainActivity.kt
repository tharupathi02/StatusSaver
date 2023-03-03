package com.leoxtech.statussaver

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import java.io.*

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerviewStatus:RecyclerView
    private lateinit var statusList:ArrayList<ModelClass>
    private lateinit var statusAdapter: StatusAdapter

    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        MobileAds.initialize(this) {}

        firebaseAnalytics = Firebase.analytics

        initial();

        val result = readDataFromPrefs()
        if (result){

            val sh = getSharedPreferences("DATA_PATH", MODE_PRIVATE);
            val uriPath = sh.getString("PATH", "");

            contentResolver.takePersistableUriPermission(Uri.parse(uriPath), Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (uriPath != null){
                val fileDoc = DocumentFile.fromTreeUri(applicationContext, Uri.parse(uriPath))
                for (file:DocumentFile in fileDoc!!.listFiles()){
                    if (!file.name!!.endsWith(".nomedia")){
                        val modelClass = ModelClass(file.name!!, file.uri.toString())
                        statusList.add(modelClass)
                    }
                }
                setUpRecyclerview(statusList)
            }
        }else{
            getFolderPermission();
        }
    }

    private fun interstitialAds() {

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this,getString(R.string.interstitial_ads), adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                    override fun onAdClicked() {
                        // Called when a click is recorded for an ad.
                    }

                    override fun onAdDismissedFullScreenContent() {
                        // Called when ad is dismissed.
                        mInterstitialAd = null
                    }

                    override fun onAdImpression() {
                        // Called when an impression is recorded for an ad.
                    }

                    override fun onAdShowedFullScreenContent() {
                        // Called when ad is shown.
                    }
                }
            }
        })

        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        }

    }

    private fun readDataFromPrefs(): Boolean {
        val sh = getSharedPreferences("DATA_PATH", MODE_PRIVATE);
        val uriPath = sh.getString("PATH", "");
        if (uriPath != null){
            if (uriPath.isEmpty()){
                return false;
            }
        }
        return true;
    }

    private fun initial() {
        recyclerviewStatus = findViewById(R.id.recyclerviewStatus);
        statusList = ArrayList();
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getFolderPermission() {
        val storageManager = application.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val intent = storageManager.primaryStorageVolume.createOpenDocumentTreeIntent()
        val targetDirectory = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"
        var uri = intent.getParcelableExtra<Uri>("android.provider.extra.INITIAL_URI") as Uri
        var scheme = uri.toString()
        scheme = scheme.replace("/root/", "/tree/")
        scheme += "%3A$targetDirectory"
        uri = Uri.parse(scheme)
        intent.putExtra("android.provider.extra.INITIAL_URI", uri)
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
        startActivityForResult(intent, 111)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK){
            val treeUri = data?.data

            val sharedPreferences = getSharedPreferences("DATA_PATH", MODE_PRIVATE);
            val myEdit = sharedPreferences.edit();
            myEdit.putString("PATH", treeUri.toString())
            myEdit.apply();

            if (treeUri != null){
                contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val fileDoc = DocumentFile.fromTreeUri(applicationContext, treeUri)
                for (file:DocumentFile in fileDoc!!.listFiles()){
                    if (!file.name!!.endsWith(".nomedia")){
                        val modelClass = ModelClass(file.name!!,file.uri.toString())
                        statusList.add(modelClass)
                    }
                }
                setUpRecyclerview(statusList)
            }
        }
    }

    private fun setUpRecyclerview(statusList: ArrayList<ModelClass>) {
        statusAdapter = applicationContext?.let {
            StatusAdapter(it, statusList) {
                selectedStatusItem:ModelClass->listItemClicked(selectedStatusItem)
            }
        }!!

        recyclerviewStatus.apply {
            setHasFixedSize(true)
            layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
            adapter = statusAdapter
        }
    }

    private fun listItemClicked(selectedStatusItem: ModelClass) {
        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle("Download & Share")
            .setMessage("Download this Status in your Storage of Share with WhatsApp")
            .setNeutralButton("Open Status") { dialog, which ->
                // Respond to neutral button press
                dialog.dismiss()
                if (selectedStatusItem.fileUri.endsWith(".mp4")) {
                    val intent = Intent(applicationContext, StatusVideoView::class.java)
                    intent.putExtra("VIDEO_URI", selectedStatusItem.fileUri)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }else{
                    val intent = Intent(applicationContext, StatusPhotoView::class.java)
                    intent.putExtra("IMAGE_URI", selectedStatusItem.fileUri)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }
            .setNegativeButton("Download") { dialog, which ->
                // Respond to negative button press
                dialog.dismiss()
                saveFile(selectedStatusItem)
                interstitialAds()
            }
            .setPositiveButton("Share WhatsApp") { dialog, which ->
                shareWhatsApp(selectedStatusItem.fileUri)
            }
            .show()
    }

    private fun shareWhatsApp(fileUri: String) {
        val imageUri = Uri.parse(fileUri)
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        //Target whatsapp:
        shareIntent.setPackage("com.whatsapp")
        //Add text and then Image URI
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
        shareIntent.type = "image/jpeg"
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            startActivity(shareIntent)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(applicationContext, ex.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun saveFile(selectedStatusItem: ModelClass) {
        if(selectedStatusItem.fileUri.endsWith(".mp4")){
            val inputStream = contentResolver.openInputStream(Uri.parse(selectedStatusItem.fileUri))
            val fileName = "${System.currentTimeMillis()}.mp4"
            try {
                val values = ContentValues()
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS+"/Videos/")
                val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
                val outputStream:OutputStream = uri?.let { contentResolver.openOutputStream(it) }!!
                if (inputStream != null){
                    outputStream.write(inputStream.readBytes())
                }
                outputStream.close()
                Toast.makeText(applicationContext, "Video Saved", Toast.LENGTH_LONG).show()
            }catch (e:IOException){
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
            }
        }else{
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.parse(selectedStatusItem.fileUri))
            val fileName = "${System.currentTimeMillis()}.jpg"
            var fos:OutputStream?=null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                contentResolver.also { resolver->
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "/image/jpg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                    val imageUri:Uri?=resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    fos = imageUri?.let { resolver.openOutputStream(it) }
                }
            }else{
                val imageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val image = File(imageDir, fileName)
                fos = FileOutputStream(image)
            }
            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                Toast.makeText(applicationContext, "Image Saved", Toast.LENGTH_LONG).show()
            }
        }
    }
}