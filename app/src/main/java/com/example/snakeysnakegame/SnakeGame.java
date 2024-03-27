package com.example.snakeysnakegame;

import static java.lang.Thread.sleep;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;


public class SnakeGame extends SurfaceView implements Runnable {
    private long mNextFrameTime;
    private volatile boolean mPlaying = true;
    private volatile boolean mPaused = true;

    // for playing sound effects
    private final SoundPool mSP;
    private int mEat_ID = -1;
    private int mCrashID = -1;

    // The size in segments of the playable area
    private final int NUM_BLOCKS_WIDE = 40;
    private final int mNumBlocksHigh;

    // How many points does the player have
    private int mScore;

    private final SurfaceHolder mSurfaceHolder;
    private final Paint mPaint;

    private final Snake mSnake;
    private final Apple mApple;

    private Bitmap mBitmapBackground;

    private Typeface mFont;

    //I had to add this, it is similar to the one found in snake.java
    //i had to import the import android.graphics.Bitmap; and import android.graphics.BitmapFactory;
    // This is the constructor method that gets called
    // from SnakeActivity
    public SnakeGame(Context context, Point size) {
        super(context);
        mFont = Typeface.createFromAsset(context.getAssets(), "minecraftfont.otf");

        mBitmapBackground = BitmapFactory.decodeResource(context.getResources(), R.drawable.background);
        // Work out how many pixels each block is
        int blockSize = size.x / NUM_BLOCKS_WIDE;
        // How many blocks of the same size will fit into the height
        mNumBlocksHigh = size.y / blockSize;
     


        // Initialize the SoundPool
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        mSP = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();
        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // Prepare the sounds in memory
            descriptor = assetManager.openFd("get_apple.ogg");
            mEat_ID = mSP.load(descriptor, 0);

            descriptor = assetManager.openFd("snake_death.ogg");
            mCrashID = mSP.load(descriptor, 0);

        } catch (IOException e) {
            // Error
        }

        // Initialize the drawing objects
        mSurfaceHolder = getHolder();
        mPaint = new Paint();

        // Call the constructors of our two game objects
        mApple = new Apple(context,new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize);

        mSnake = new Snake(context, new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh),blockSize);
    }


    // Called to start a new game
    public void newGame() {

        // reset the snake
        mSnake.reset(NUM_BLOCKS_WIDE, mNumBlocksHigh);

        // Get the apple ready for dinner
        mApple.spawn();

        // Reset the mScore
        mScore = 0;

        // Setup mNextFrameTime so an update can triggered
        mNextFrameTime = System.currentTimeMillis();
    }

    public void pauseGame() {
        mPaused = true;
    }

    // Handles the game loop
    @Override
    public void run() {
        while (mPlaying) {
            if (!mPaused) {
                // Update 10 times a second
                if (updateRequired()) {
                    update();
                }
            }

            draw();
            // Add a delay to control the frame rate
            try {
                sleep(10); // Adjust the delay as needed
            } catch (InterruptedException e) {
                //  e.printStackTrace();
            }
        }
    }


    // Check to see if it is time for an update
    public boolean updateRequired() {

        // Run at 10 frames per second
        final long TARGET_FPS = 10;
        // There are 1000 milliseconds in a second
        final long MILLIS_PER_SECOND = 1000;

        // Are we due to update the frame
        if (mNextFrameTime <= System.currentTimeMillis()) {
            // Tenth of a second has passed

            // Setup when the next update will be triggered
            mNextFrameTime = System.currentTimeMillis()
                    + MILLIS_PER_SECOND / TARGET_FPS;

            // Return true so that the update and draw
            // methods are executed
            return true;
        }

        return false;
    }


    // Update all the game objects
    public void update() {
        if (mPaused) {
            return;
        }

        // Move the snake
        mSnake.move();

        // Did the head of the snake eat the apple?
        if (mSnake.checkDinner(mApple.getLocation())) {
            // This reminds me of Edge of Tomorrow.
            // One day the apple will be ready!
            mApple.spawn();

            // Add to  mScore
            mScore = mScore + 1;

            // Play a sound
            mSP.play(mEat_ID, 1, 1, 0, 0, 1);
        }

        // Did the snake die?
        if (mSnake.detectDeath()) {
            // Pause the game ready to start again
            mSP.play(mCrashID, 1, 1, 0, 0, 1);

            mPaused = true;
        }

    }


    // Do all the drawing
    public void draw() {
        // Get a lock on the mCanvas
        if (mSurfaceHolder.getSurface().isValid()) {
            // Objects for drawing
            Canvas mCanvas = mSurfaceHolder.lockCanvas();

            mPaint.setTypeface(mFont);
            // Fill the screen with a color
            mCanvas.drawBitmap(mBitmapBackground, 0, 0, null);
            mBitmapBackground = Bitmap.createScaledBitmap(mBitmapBackground, 1080, 2220, false);
            // Set the size and color of the mPaint for the text
            mPaint.setColor(Color.argb(255, 0, 0, 0));
            mPaint.setTextSize(120);

            // Draw the score
            mCanvas.drawText("SCORE:" + mScore, 60, 160, mPaint);

            // Draw the apple and the snake
            mApple.draw(mCanvas, mPaint);
            mSnake.draw(mCanvas, mPaint);

            // Set the size and color of the mPaint for the text
            mPaint.setColor(Color.argb(255, 0, 0, 0));
            mPaint.setTextSize(67);
            mCanvas.drawText("PAUSE", 64, 265, mPaint);

            // Draw some text while paused
            if (mPaused) {
                mCanvas.drawText("TAP TO PLAY!", 300, 1000, mPaint);
             
            }
            mPaint.setTextSize(35);
            mPaint.setColor(Color.argb(255, 0, 0, 0));
            mCanvas.drawText("MARIA VALENCIA", 727, 75, mPaint);
            mCanvas.drawText("MARILYN SARABIA", 719, 120, mPaint);
            // Unlock the mCanvas and reveal the graphics for this frame
            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
        }
    }
    // Other code remains the same...

    // Handle touch events
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if ((motionEvent.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            // Get the coordinates of the touch event
            int touchX = (int) motionEvent.getX();
            int touchY = (int) motionEvent.getY();

            // Check if the touch event is within the bounds of the "Pause" text
            if (touchX >= 100 && touchX <= 100 + mPaint.measureText("Pause") &&
                    touchY >= 200 - mPaint.getTextSize() && touchY <= 200) {
                // Toggle the pause state
                togglePause();
                return true;
            } else if (mPaused) {
                // If the game is paused and the touch event is outside the "Pause" text,
                // resume the game
                mPaused = false;
                newGame();
                return true;
            } else {
                // If the game is not paused, handle snake direction change
                mSnake.switchHeading(motionEvent);
                return true;
            }
        }

        return true;
    }

    // Stop the thread
    public void togglePause() {
        mPaused = !mPaused;
    }

    // Start the thread
    public void resume() {
        mPlaying = true;
        // Objects for the game loop/thread
        Thread mThread = new Thread(this);
        mThread.start(); // Start the thread
    }
}
