package example.com.super_res;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import static android.R.attr.bitmap;
import static android.R.attr.x;
import static android.R.attr.y;


public class MainActivity extends AppCompatActivity {

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int PICK_IMAGE_REQUEST = 2;
    private static final String MODEL_FILE = "file:///android_asset/test_graph.pb";//"file:///android_asset/stylize_quantized.pb";

    private static final String INPUT_NODE = "input_3"; //"input"
    private static final String OUTPUT_NODE = "mul_245";//"transformer/expand/conv3/conv/Sigmoid";
    private static final String Res_NODE = "style_num";
    private static final boolean DEBUG_MODEL = false;
    private static final int NUM_STYLES = 26;

    ImageView mImageView;
    String mCurrentPhotoPath;
    Bitmap mBitmap;
    private float[] floatValues;
    private float[] floatValues2;
    private int[] intValues;
    private int[] intValues2;
    private int frameNum = 1; //?
    private int startSize = 200;
    private int desiredSize = 4*startSize;
    private  double[] rn_mean = new double[] {123.68, 116.779, 103.939};
    private final float[] styleVals = new float[NUM_STYLES];

    private TensorFlowInferenceInterface inferenceInterface;

    static {
        System.loadLibrary("tensorflow_inference");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView) findViewById(R.id.image);

        inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);

        intValues = new int[startSize * startSize];
        floatValues = new float[startSize * startSize * 3];
        intValues2 = new int[desiredSize * desiredSize];
        floatValues2 = new float[desiredSize * desiredSize * 3];

        for (int i = 0; i < NUM_STYLES; ++i) {
            styleVals[i] = 1.0f / NUM_STYLES;
        }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Pick Image from")
                .setPositiveButton("Gallery", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent();
                        // Show only images, no videos or anything else
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        // Always show the chooser (if there are multiple options available)
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
        myRec();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode){
                case (REQUEST_TAKE_PHOTO): {
                    setPic();
                    break;
                }
                case (PICK_IMAGE_REQUEST): {
                    Uri selectedimg = data.getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedimg);
                        mBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        mImageView.setImageBitmap(mBitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e("Main activity", ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "example.com.super_res.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        mImageView.setImageBitmap(mBitmap);
    }

    public void superRes(View view){
        int x1 = (mBitmap.getWidth() - startSize)/2;
        int y1 = (mBitmap.getHeight() - startSize)/2;
        int w1, h1;
        if ((mBitmap.getWidth() - startSize)%2 == 1){
            w1 = mBitmap.getWidth() - 2*x1 - 1;
        }
        else {
            w1 = mBitmap.getWidth() - 2*x1;
        }
        if ((mBitmap.getHeight() - startSize)%2 == 1){
            h1 = mBitmap.getHeight() - 2*y1 - 1;
        }
        else {
            h1 = mBitmap.getHeight() - 2*y1;
        }
        Bitmap croppedBitmap = Bitmap.createBitmap(mBitmap, x1, y1 , w1, h1);
        Bitmap croppedBitmap2 = croppedBitmap.copy(Bitmap.Config.ARGB_8888, true);
        croppedBitmap2.getPixels(intValues, 0, croppedBitmap2.getWidth(), 0, 0,
                croppedBitmap2.getWidth(), croppedBitmap2.getHeight());

//        if (DEBUG_MODEL) {
//            // Create a white square that steps through a black background 1 pixel per frame.
//            final int centerX = (frameNum + mBitmap.getWidth() / 2) % mBitmap.getWidth();
//            final int centerY = mBitmap.getHeight() / 2;
//            final int squareSize = 10;
//            for (int i = 0; i < intValues.length; ++i) {
//                final int x = i % mBitmap.getWidth();
//                final int y = i / mBitmap.getHeight();
//                final float val =
//                        Math.abs(x - centerX) < squareSize && Math.abs(y - centerY) < squareSize ? 1.0f : 0.0f;
//                floatValues[i * 3] = val;
//                floatValues[i * 3 + 1] = val;
//                floatValues[i * 3 + 2] = val;
//            }
//        } else {
//            for (int i = 0; i < intValues.length; ++i) {
//                final int val = intValues[i];
//                floatValues[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
//                floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
//                floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f;
//            }
//        }
        for (int i = 0; i < intValues.length; ++i) {
                final int val = intValues[i];
                floatValues[i * 3 ] = ((val >> 16) & 0xFF);// - (float) rn_mean[0];
                floatValues[i * 3 + 1] = ((val >> 8) & 0xFF);// - (float) rn_mean[1];
                floatValues[i * 3 + 2] = (val & 0xFF);// - (float) rn_mean[2];
            }
        inferenceInterface.feed(INPUT_NODE, floatValues, 1, croppedBitmap2.getWidth(), croppedBitmap2.getHeight(), 3);

        Log.d("po feed", "?");
        inferenceInterface.run(new String[] {OUTPUT_NODE}, false);
        Log.d("po run", "?");
        inferenceInterface.fetch(OUTPUT_NODE, floatValues2);
        Log.d("koniec", "po fetch");


        for (int i = 0; i < intValues2.length; ++i) {
            intValues2[i] =
                    0xFF000000
                            | (Math.min((int) (floatValues2[i * 3 ]  ), 255) << 16)
                            | (Math.min((int) (floatValues2[i * 3 + 1]  ), 255) << 8)
                            | (Math.min((int) (floatValues2[i * 3 + 2]  ), 255));
        }

//        for (int i = 0; i < intValues.length; ++i) {
//            intValues[i] =
//                    0xFF000000
//                            | (((int) (floatValues[i * 3] + (float) rn_mean[0])) << 16)
//                            | (((int) (floatValues[i * 3 + 1] + (float) rn_mean[1])) << 8)
//                            | ((int) (floatValues[i * 3 + 2] + (float) rn_mean[2]));
//        }

        Bitmap newBitmap = Bitmap.createBitmap(desiredSize, desiredSize, Bitmap.Config.ARGB_8888);;

        newBitmap.setPixels(intValues2, 0, newBitmap.getWidth(), 0, 0, newBitmap.getWidth(), newBitmap.getHeight());
        mBitmap = newBitmap;
        mImageView.setImageBitmap(mBitmap);
        Log.d("koniec", "sam koniec");
    }

    public void myRec(){
        Bitmap b = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint myPaint = new Paint();
        myPaint.setStyle(Paint.Style.STROKE);
        myPaint.setStrokeWidth(10);
        c.drawRect(100, 100, 200, 200, myPaint);

    }
}