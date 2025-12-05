package com.example.notification

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.viewpager2.widget.ViewPager2

class StoryActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var progressContainer: LinearLayout
    private lateinit var imageUrls: ArrayList<String>
    private var currentStoryIndex = 0
    private var storyTimer: CountDownTimer? = null
    private var progressBars: Array<ProgressBar?> = arrayOf()
    private var remainingTime: Long = STORY_DURATION
    private var isPaused = false

    companion object {
        private const val STORY_DURATION = 5000L // 5 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story)

        viewPager = findViewById(R.id.storyViewPager)
        progressContainer = findViewById(R.id.progressContainer)
        val reverseView = findViewById<View>(R.id.reverseView)
        val skipView = findViewById<View>(R.id.skipView)

        imageUrls = intent.getStringArrayListExtra("IMAGE_URLS") ?: ArrayList()

        if (imageUrls.isNotEmpty()) {
            setupViewPager()
            setupProgressBars()
            setupClickListeners(reverseView, skipView)
        }
    }

    override fun onResume() {
        super.onResume()
        if(!isPaused) startStoryTimer(remainingTime)
    }

    override fun onPause() {
        super.onPause()
        storyTimer?.cancel()
    }

    private fun setupViewPager() {
        viewPager.adapter = StoryImageAdapter(this, imageUrls)
        viewPager.isUserInputEnabled = false // Disable swipe
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentStoryIndex = position
                resetStoryTimer()
            }
        })
    }

    private fun setupProgressBars() {
        progressBars = arrayOfNulls(imageUrls.size)
        progressContainer.removeAllViews()
        for (i in imageUrls.indices) {
            val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = 8
                }
                max = 100
            }
            progressContainer.addView(progressBar)
            progressBars[i] = progressBar
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupClickListeners(reverseView: View, skipView: View) {
        val reverseGestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                showPreviousStory()
                return true
            }
            override fun onLongPress(e: MotionEvent) {
                pauseStory()
            }
        })

        val skipGestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                showNextStory()
                return true
            }
            override fun onLongPress(e: MotionEvent) {
                pauseStory()
            }
        })

        reverseView.setOnTouchListener { _, event ->
            reverseGestureDetector.onTouchEvent(event)
            if(event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) resumeStory()
            true
        }

        skipView.setOnTouchListener { _, event ->
            skipGestureDetector.onTouchEvent(event)
            if(event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) resumeStory()
            true
        }
    }
    
    private fun pauseStory() {
        isPaused = true
        storyTimer?.cancel()
    }
    
    private fun resumeStory() {
        if(isPaused) {
            isPaused = false
            startStoryTimer(remainingTime)
        }
    }

    private fun startStoryTimer(duration: Long) {
        storyTimer?.cancel()
        val currentProgressBar = progressBars.getOrNull(currentStoryIndex)
        storyTimer = object : CountDownTimer(duration, 50) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                val progress = (((STORY_DURATION - millisUntilFinished) * 100) / STORY_DURATION).toInt()
                currentProgressBar?.progress = progress
            }

            override fun onFinish() {
                showNextStory()
            }
        }.start()
    }

    private fun resetStoryTimer() {
        for (i in 0 until progressBars.size) {
            val progressBar = progressBars.getOrNull(i)
            if (i < currentStoryIndex) progressBar?.progress = 100 else progressBar?.progress = 0
        }
        startStoryTimer(STORY_DURATION)
    }

    private fun showNextStory() {
        if (currentStoryIndex < imageUrls.size - 1) {
            viewPager.setCurrentItem(++currentStoryIndex, false)
        } else {
            finish()
        }
    }

    private fun showPreviousStory() {
        if (currentStoryIndex > 0) {
            viewPager.setCurrentItem(--currentStoryIndex, false)
        }
    }
}
