package com.asapp.live

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "Asapp v0.1\nCamera streaming is not enabled yet."
            textSize = 22f
            setPadding(32, 64, 32, 32)
        })
    }
}
