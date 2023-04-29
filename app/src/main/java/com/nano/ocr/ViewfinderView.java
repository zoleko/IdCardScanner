/*
 * Copyright (C) 2008 ZXing authors
 * Copyright 2011 Robert Theis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nano.ocr;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.nano.ocr.camera.CameraManager;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the result text.
 *
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing
 */
public final class ViewfinderView extends View {


  private CameraManager cameraManager;
  private final Paint paint;
  private final int maskColor;
  private final int frameColor;
  private final int cornerColor;


  // This constructor is used when the class is built from an XML resource.
  public ViewfinderView(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Initialize these once for performance rather than calling them every time in onDraw().
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Resources resources = getResources();
    maskColor = resources.getColor(R.color.viewfinder_mask);
    frameColor = resources.getColor(R.color.viewfinder_frame);
    cornerColor = resources.getColor(R.color.viewfinder_corners);

  }

  public void setCameraManager(CameraManager cameraManager) {
    this.cameraManager = cameraManager;
  }

  @SuppressWarnings("unused")
  @Override
  public void onDraw(Canvas canvas) {
    Rect frame = cameraManager.getFramingRect();
    if (frame == null) {
      return;
    }
    int width = canvas.getWidth();
    int height = canvas.getHeight(); 

    // Draw the exterior (i.e. outside the framing rect) darkened
    paint.setColor(maskColor);
    canvas.drawRect(0, 0, width, frame.top, paint);
    canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1.0f, paint);
    canvas.drawRect(frame.right + 1.0f, frame.top, width, frame.bottom + 1.0f, paint);
    canvas.drawRect(0, frame.bottom + 1.0f, width, height, paint);

    // If we have an OCR result, overlay its information on the viewfinder.

    // Draw a two pixel solid border inside the framing rect
    paint.setAlpha(0);
    paint.setStyle(Style.FILL);
    paint.setColor(frameColor);
    canvas.drawRect(frame.left, frame.top, frame.right + 1.0f, frame.top + 2.0f, paint);
    canvas.drawRect(frame.left, frame.top + 2.0f, frame.left + 2.0f, frame.bottom - 1.0f, paint);
    canvas.drawRect(frame.right - 1.0f, frame.top, frame.right + 1.0f, frame.bottom - 1.0f, paint);
    canvas.drawRect(frame.left, frame.bottom - 1.0f, frame.right + 1.0f, frame.bottom + 1.0f, paint);

    // Draw the framing rect corner UI elements
    paint.setColor(cornerColor);
    canvas.drawRect(frame.left - 15.0f, frame.top - 15.0f, frame.left + 15.0f, frame.top, paint);
    canvas.drawRect(frame.left - 15.0f, frame.top, frame.left, frame.top + 15.0f, paint);
    canvas.drawRect(frame.right - 15.0f, frame.top - 15.0f, frame.right + 15.0f, frame.top, paint);
    canvas.drawRect(frame.right, frame.top - 15.0f, frame.right + 15.0f, frame.top + 15.0f, paint);
    canvas.drawRect(frame.left - 15.0f, frame.bottom, frame.left + 15.0f, frame.bottom + 15.0f, paint);
    canvas.drawRect(frame.left - 15.0f, frame.bottom - 15.0f, frame.left, frame.bottom, paint);
    canvas.drawRect(frame.right - 15.0f, frame.bottom, frame.right + 15.0f, frame.bottom + 15.0f, paint);
    canvas.drawRect(frame.right, frame.bottom - 15.0f, frame.right + 15.0f, frame.bottom + 15.0f, paint);


    // Request another update at the animation interval, but don't repaint the entire viewfinder mask.
    //postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
  }

  public void drawViewfinder() {
    invalidate();
  }


}
