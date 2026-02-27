package com.zarkit.zarkit_partner.UtilityClasses

import android.content.Context
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class Utility {
    companion object{
        fun changeFragment(
            context: Context,
            container:FrameLayout, fragment: Fragment
        ){
            val fragmentManager: FragmentManager = (context as AppCompatActivity).supportFragmentManager
            fragmentManager.beginTransaction().replace(container.id, fragment).commit()
        }
    }
}