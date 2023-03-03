package com.leoxtech.statussaver

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class StatusVideoView : AppCompatActivity(), Player.Listener {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    private lateinit var mAdView : AdView
    private var mInterstitialAd: InterstitialAd? = null

    var videoUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status_video_view)

        MobileAds.initialize(this) {}

        initial()

        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        player.addListener(this)

        addMp4File()

        bannerAds()

        interstitialAds()

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

    private fun bannerAds() {

        val adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = getString(R.string.banner_ads)
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        mAdView.adListener = object: AdListener() {
            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }

            override fun onAdFailedToLoad(adError : LoadAdError) {
                // Code to be executed when an ad request fails.
            }

            override fun onAdImpression() {
                // Code to be executed when an impression is recorded
                // for an ad.
            }

            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }
        }

    }


    private fun initial() {
        playerView = findViewById(R.id.playerView)
    }

    private fun addMp4File(){
        val uri = intent.getStringExtra("VIDEO_URI")
        videoUrl = uri
        val mediaItem = MediaItem.fromUri(uri.toString())
        player.setMediaItem(mediaItem)
        player.prepare()
        playerView.hideController()
        player.play()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)

        when(playbackState){
            Player.STATE_BUFFERING -> {
                // Buffering
            }
            Player.STATE_ENDED -> {
                // Playback ended
            }
            Player.STATE_IDLE -> {
                // Idle
            }
            Player.STATE_READY -> {
                // Ready
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        player.release()
    }

    override fun onStop() {
        super.onStop()
        player.release()
    }
}