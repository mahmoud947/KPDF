package com.mahmoud.kpdf_core.image

/*
 * Created by Mahmoud Kamal El-Din on 2026-04-22.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

expect class KPlatformImage {
    constructor(width: Int, height: Int)

    val width: Int
    val height: Int
}