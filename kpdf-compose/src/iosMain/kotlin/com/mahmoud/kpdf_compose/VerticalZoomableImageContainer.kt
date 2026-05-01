package com.mahmoud.kpdf_compose

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIScrollView
import platform.UIKit.UIScrollViewDelegateProtocol
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIView
import platform.UIKit.UIViewContentMode.UIViewContentModeScaleAspectFit
import platform.darwin.NSObject
import kotlin.math.abs

/*
 * Created by Mahmoud Kamal El-Din on 01/05/2026.
 * Copyright (c) 2026 KDF. All rights reserved.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class VerticalZoomableImageContainer(
    frame: CValue<CGRect>,
    private val minZoom: Double = 1.0,
    private val maxZoom: Double = 4.0,
    private val doubleTapZoom: Double = 2.0,
) : UIView(frame) {
    var onZoomChanged: ((Double) -> Unit)? = null

    private var lastImage: UIImage? = null

    private val imageView = UIImageView().apply {
        contentMode = UIViewContentModeScaleAspectFit
        clipsToBounds = true
        userInteractionEnabled = true
    }

    private val zoomDelegate = object : NSObject(), UIScrollViewDelegateProtocol {
        override fun viewForZoomingInScrollView(scrollView: UIScrollView): UIView? = imageView

        override fun scrollViewDidZoom(scrollView: UIScrollView) {
            centerImage()
            updateScrollEnabled()
            onZoomChanged?.invoke(scrollView.zoomScale)
        }
    }

    private val scrollView = UIScrollView().apply {
        clipsToBounds = true
        showsHorizontalScrollIndicator = false
        showsVerticalScrollIndicator = false
        bouncesZoom = true
        minimumZoomScale = minZoom
        maximumZoomScale = maxZoom
        delegate = zoomDelegate
        addSubview(imageView)
    }

    init {
        addSubview(scrollView)

        val doubleTap = UITapGestureRecognizer(
            target = this,
            action = NSSelectorFromString("handleDoubleTap:")
        ).apply {
            numberOfTapsRequired = 2u
        }

        scrollView.addGestureRecognizer(doubleTap)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()

        scrollView.setFrame(bounds)
        imageView.setFrame(scrollView.bounds)
        scrollView.setContentSize(scrollView.bounds.useContents {
            CGSizeMake(size.width, size.height)
        })
        centerImage()
    }

    fun setImage(image: UIImage?) {
        if (lastImage === image) return
        lastImage = image
        imageView.image = image
        scrollView.setZoomScale(minZoom, animated = false)
        updateScrollEnabled()
        centerImage()
        onZoomChanged?.invoke(scrollView.zoomScale)
    }

    fun setExternalZoom(scale: Double, animated: Boolean) {
        val boundedScale = scale.coerceIn(minZoom, maxZoom)
        if (abs(scrollView.zoomScale - boundedScale) < 0.01) return
        scrollView.setZoomScale(boundedScale, animated = animated)
        updateScrollEnabled()
    }

    @ObjCAction
    fun handleDoubleTap(recognizer: UITapGestureRecognizer) {
        val currentZoom = scrollView.zoomScale
        val targetZoom = if (currentZoom > minZoom) {
            minZoom
        } else {
            doubleTapZoom.coerceIn(minZoom, maxZoom)
        }

        if (targetZoom == minZoom) {
            scrollView.setZoomScale(minZoom, animated = true)
            return
        }

        val tapPoint = recognizer.locationInView(imageView)
        val zoomRect = zoomRectForScale(
            scale = targetZoom,
            center = tapPoint,
        )

        scrollView.zoomToRect(zoomRect, animated = true)
    }

    private fun zoomRectForScale(
        scale: Double,
        center: CValue<CGPoint>,
    ): CValue<CGRect> {
        return scrollView.bounds.useContents {
            center.useContents {
                val width = size.width / scale
                val height = size.height / scale

                CGRectMake(
                    x = this.x - width / 2.0,
                    y = this.y - height / 2.0,
                    width = width,
                    height = height,
                )
            }
        }
    }

    private fun centerImage() {
        scrollView.bounds.useContents {
            val scrollWidth = size.width
            val scrollHeight = size.height

            imageView.frame.useContents {
                val imageWidth = size.width
                val imageHeight = size.height

                val horizontalInset =
                    if (imageWidth < scrollWidth) (scrollWidth - imageWidth) / 2.0 else 0.0
                val verticalInset =
                    if (imageHeight < scrollHeight) (scrollHeight - imageHeight) / 2.0 else 0.0

                scrollView.contentInset = platform.UIKit.UIEdgeInsetsMake(
                    top = verticalInset,
                    left = horizontalInset,
                    bottom = verticalInset,
                    right = horizontalInset,
                )
            }
        }
    }

    private fun updateScrollEnabled() {
        scrollView.scrollEnabled = scrollView.zoomScale > minZoom + 0.01
    }
}
