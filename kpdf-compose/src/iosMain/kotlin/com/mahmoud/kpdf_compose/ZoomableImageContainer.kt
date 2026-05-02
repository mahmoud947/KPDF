package com.mahmoud.kpdf_compose
import com.mahmoud.kpdf_core.api.KPdfSearchRect
import kotlinx.cinterop.CValue
import kotlinx.cinterop.BetaInteropApi
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
import platform.UIKit.UIColor
import platform.UIKit.UIScrollView
import platform.UIKit.UIScrollViewDelegateProtocol
import platform.UIKit.UISwipeGestureRecognizer
import platform.UIKit.UISwipeGestureRecognizerDirectionLeft
import platform.UIKit.UISwipeGestureRecognizerDirectionRight
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIView
import platform.UIKit.UIViewContentMode.UIViewContentModeScaleAspectFit
import platform.darwin.NSObject
import kotlin.math.abs

/*
 * Created by Mahmoud Kamal El-Din on 23/04/2026.
 * Copyright (c) 2026 KDF. All rights reserved.
 */

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class ZoomableImageContainer(
    frame: CValue<CGRect>,
    private val minZoom: Double = 1.0,
    private val maxZoom: Double = 4.0,
    private val doubleTapZoom: Double = 2.0,
    private val swipeEnabled: Boolean = true,
) : UIView(frame) {
    var onZoomChanged: ((Double) -> Unit)? = null
    var onSwipeNext: (() -> Unit)? = null
    var onSwipePrevious: (() -> Unit)? = null

    private var lastImage: UIImage? = null
    private var searchHighlights: List<KPdfSearchHighlight> = emptyList()
    private val highlightViews = mutableListOf<UIView>()

    private val imageView = UIImageView().apply {
        contentMode = UIViewContentModeScaleAspectFit
        clipsToBounds = true
        userInteractionEnabled = true
    }

    private val zoomDelegate = object : NSObject(), UIScrollViewDelegateProtocol {
        override fun viewForZoomingInScrollView(scrollView: UIScrollView): UIView? {
            return imageView
        }

        override fun scrollViewDidZoom(scrollView: UIScrollView) {
            centerImage()
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

        if (swipeEnabled) {
            scrollView.addGestureRecognizer(
                UISwipeGestureRecognizer(
                    target = this,
                    action = NSSelectorFromString("handleSwipeNext:")
                ).apply {
                    direction = UISwipeGestureRecognizerDirectionLeft
                }
            )
            scrollView.addGestureRecognizer(
                UISwipeGestureRecognizer(
                    target = this,
                    action = NSSelectorFromString("handleSwipePrevious:")
                ).apply {
                    direction = UISwipeGestureRecognizerDirectionRight
                }
            )
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()

        scrollView.setFrame(bounds)
        imageView.setFrame(scrollView.bounds)
        scrollView.setContentSize(scrollView.bounds.useContents {
            CGSizeMake(size.width, size.height)
        })
        centerImage()
        layoutSearchHighlights()
    }

    fun setImage(image: UIImage?) {
        if (lastImage === image) return
        lastImage = image
        imageView.image = image
        scrollView.setZoomScale(minZoom, animated = false)
        centerImage()
        layoutSearchHighlights()
        onZoomChanged?.invoke(scrollView.zoomScale)
    }

    fun setSearchHighlights(highlights: List<KPdfSearchHighlight>) {
        searchHighlights = highlights
        layoutSearchHighlights()
    }

    fun setExternalZoom(scale: Double, animated: Boolean) {
        val boundedScale = scale.coerceIn(minZoom, maxZoom)
        if (kotlin.math.abs(scrollView.zoomScale - boundedScale) < 0.01) return
        scrollView.setZoomScale(boundedScale, animated = animated)
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
            center = tapPoint
        )

        scrollView.zoomToRect(zoomRect, animated = true)
    }

    @ObjCAction
    fun handleSwipeNext(recognizer: UISwipeGestureRecognizer) {
        if (!canSwipe()) return
        onSwipeNext?.invoke()
    }

    @ObjCAction
    fun handleSwipePrevious(recognizer: UISwipeGestureRecognizer) {
        if (!canSwipe()) return
        onSwipePrevious?.invoke()
    }

    private fun zoomRectForScale(
        scale: Double,
        center: CValue<CGPoint>
    ): CValue<CGRect> {
        return scrollView.bounds.useContents {
            center.useContents {
                val width = size.width / scale
                val height = size.height / scale

                CGRectMake(
                    x = this.x - width / 2.0,
                    y = this.y - height / 2.0,
                    width = width,
                    height = height
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
                    right = horizontalInset
                )
            }
        }
    }

    private fun layoutSearchHighlights() {
        highlightViews.forEach { it.removeFromSuperview() }
        highlightViews.clear()

        val image = lastImage ?: return
        if (searchHighlights.isEmpty()) return

        val imageRect = fittedImageRect(image)
        searchHighlights.forEach { highlight ->
            val rect = highlight.rect
            val view = UIView(
                frame = CGRectMake(
                    x = imageRect.x + rect.left * imageRect.width,
                    y = imageRect.y + rect.top * imageRect.height,
                    width = ((rect.right - rect.left) * imageRect.width).coerceAtLeast(1.0),
                    height = ((rect.bottom - rect.top) * imageRect.height).coerceAtLeast(1.0),
                )
            ).apply {
                backgroundColor = if (highlight.active) {
                    UIColor.orangeColor.colorWithAlphaComponent(0.54)
                } else {
                    UIColor.yellowColor.colorWithAlphaComponent(0.45)
                }
                userInteractionEnabled = false
                layer.cornerRadius = 3.0
            }

            imageView.addSubview(view)
            highlightViews += view
        }
    }

    private fun fittedImageRect(image: UIImage): ImageRect {
        val viewSize = imageView.bounds.useContents { size.width to size.height }
        val imageSize = image.size.useContents { width to height }
        val viewWidth = viewSize.first.coerceAtLeast(1.0)
        val viewHeight = viewSize.second.coerceAtLeast(1.0)
        val imageWidth = imageSize.first.coerceAtLeast(1.0)
        val imageHeight = imageSize.second.coerceAtLeast(1.0)
        val imageAspect = imageWidth / imageHeight
        val viewAspect = viewWidth / viewHeight

        return if (imageAspect > viewAspect) {
            val fittedHeight = viewWidth / imageAspect
            ImageRect(
                x = 0.0,
                y = (viewHeight - fittedHeight) / 2.0,
                width = viewWidth,
                height = fittedHeight,
            )
        } else {
            val fittedWidth = viewHeight * imageAspect
            ImageRect(
                x = (viewWidth - fittedWidth) / 2.0,
                y = 0.0,
                width = fittedWidth,
                height = viewHeight,
            )
        }
    }

    private fun canSwipe(): Boolean =
        swipeEnabled && abs(scrollView.zoomScale - minZoom) < 0.01
}

data class KPdfSearchHighlight(
    val rect: KPdfSearchRect,
    val active: Boolean,
)

private data class ImageRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)
