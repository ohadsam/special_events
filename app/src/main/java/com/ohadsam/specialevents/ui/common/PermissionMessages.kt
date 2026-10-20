package com.ohadsam.specialevents.ui.common

import android.content.Context
import android.widget.Toast
import com.ohadsam.specialevents.R

/** A denied permission must not look like a button that does nothing: say so, and where to change it. */
fun permissionDeniedToast(context: Context) {
    Toast.makeText(context, R.string.permission_denied, Toast.LENGTH_LONG).show()
}
