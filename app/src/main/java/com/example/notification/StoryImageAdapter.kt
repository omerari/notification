package com.example.notification

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.load
import okhttp3.Credentials
import okhttp3.OkHttpClient

class StoryImageAdapter(private val context: Context, private val imageUrls: List<String>) : RecyclerView.Adapter<StoryImageAdapter.ImageViewHolder>() {

    private val imageLoader by lazy {
        ImageLoader.Builder(context)
            .okHttpClient {
                // Get credentials from SharedPreferences for every request
                val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val username = sharedPreferences.getString("username", "")!!
                val password = sharedPreferences.getString("password", "")!!

                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val original = chain.request()
                        val requestBuilder = original.newBuilder()
                            .header("Authorization", Credentials.basic(username, password))
                        val request = requestBuilder.build()
                        chain.proceed(request)
                    }
                    .build()
            }
            .build()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_story_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        // Use the custom imageLoader to load the image
        holder.imageView.load(imageUrls[position], imageLoader)
    }

    override fun getItemCount(): Int = imageUrls.size

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.storyImageView)
    }
}
