package com.indiacybercafe.printhub.utils

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.indiacybercafe.printhub.R
import com.indiacybercafe.printhub.UploadProcessingActivity
import kotlinx.coroutines.launch

class GlobalUploadObserver(private val activity: AppCompatActivity) {

    private var miniWidget: View? = null

    fun startObserving() {
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                UploadManager.uploadState.collect { state ->
                    updateWidget(state)
                }
            }
        }
    }

    private fun updateWidget(state: UploadState) {
        val rootLayout = activity.findViewById<ViewGroup>(android.R.id.content)
        
        // Don't show mini widget in UploadProcessingActivity itself
        if (activity is UploadProcessingActivity) {
            removeWidget(rootLayout)
            return
        }

        if (state.isVisible && state.status == UploadStatus.UPLOADING) {
            if (miniWidget == null) {
                miniWidget = LayoutInflater.from(activity).inflate(R.layout.widget_upload_mini, rootLayout, false)
                
                // Position it above bottom navigation if it exists
                val params = miniWidget!!.layoutParams as ViewGroup.MarginLayoutParams
                params.bottomMargin = getBottomNavHeight()
                
                rootLayout.addView(miniWidget)
                
                miniWidget?.findViewById<Button>(R.id.btnMiniView)?.setOnClickListener {
                    val intent = Intent(activity, UploadProcessingActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    activity.startActivity(intent)
                }
                
                miniWidget?.findViewById<ImageView>(R.id.btnMiniClose)?.setOnClickListener {
                    UploadManager.dismissWidget(activity)
                }
            }
            
            miniWidget?.visibility = View.VISIBLE
            miniWidget?.findViewById<TextView>(R.id.tvMiniPercentage)?.text = "${state.progressPercentage}%"
            miniWidget?.findViewById<TextView>(R.id.tvMiniFileName)?.text = state.currentFileName
            miniWidget?.findViewById<ProgressBar>(R.id.pbMiniUpload)?.progress = state.progressPercentage
            miniWidget?.findViewById<TextView>(R.id.tvMiniCount)?.text = "${state.uploadedFiles + 1}/${state.totalFiles} files"
            
        } else {
            removeWidget(rootLayout)
        }
    }

    private fun removeWidget(rootLayout: ViewGroup) {
        miniWidget?.let {
            rootLayout.removeView(it)
            miniWidget = null
        }
    }

    private fun getBottomNavHeight(): Int {
        val bottomNav = activity.findViewById<View>(R.id.bottom_navigation)
        return bottomNav?.height ?: 0
    }
}
