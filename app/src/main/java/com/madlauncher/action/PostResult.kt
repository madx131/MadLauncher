package com.madlauncher.action

/** Result of attempting a social post through Action Box `!`. */
enum class PostResult {
    Started,
    /** Accessibility is opening compose and inserting text. */
    Automating,
    /** Copied + opened; the app blocks filling from MAD (Facebook / Instagram). */
    PasteManual,
    Failed,
}
