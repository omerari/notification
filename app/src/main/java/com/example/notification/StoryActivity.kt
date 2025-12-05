package com.example.notification

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class StoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story)

        val imageUrls = intent.getStringArrayListExtra("IMAGE_URLS")
        val viewPager: ViewPager2 = findViewById(R.id.storyViewPager)

        if (imageUrls != null) {
            viewPager.adapter = StoryImageAdapter(this, imageUrls)
        }
    }
}
