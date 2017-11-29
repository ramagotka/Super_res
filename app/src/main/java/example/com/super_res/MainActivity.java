package example.com.super_res;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import static android.R.attr.y;


public class MainActivity extends AppCompatActivity {

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int PICK_IMAGE_REQUEST = 2;
    private static final String MODEL_FILE = "file:///android_asset/test_graph.pb";//"file:///android_asset/stylize_quantized.pb";
    static final int WRITE_EXTERNAL_STORAGE = 3;

    private static final String INPUT_NODE = "input_3"; //"input"
    private static final String OUTPUT_NODE = "mul_245";//"transformer/expand/conv3/conv/Sigmoid";
    private static final String Res_NODE = "style_num";
    private static final boolean DEBUG_MODEL = false;
    private static final int NUM_STYLES = 26;

    FrameLayout mFrameLayout;
    String mCurrentPhotoPath;
    Bitmap mBitmap;
    private float[] floatValues;
    private float[] floatValues2;
    private int[] intValues;
    private int[] intValues2;
    private int startSize = 200;
    private int desiredSize = 4*startSize;
    private  double[] rn_mean = new double[] {123.68, 116.779, 103.939};
    private final float[] styleVals = new float[NUM_STYLES];
    private MyDragView myRect;
    private Point mPosition;
    MyDragView myDragView;
    private Button mButton;
    private Button mButtonSave;
    private ShareActionProvider mShareActionProvider;
    private boolean mShare;

    private TensorFlowInferenceInterface inferenceInterface;

    static {
        System.loadLibrary("tensorflow_inference");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFrameLayout = (FrameLayout) findViewById(R.id.image);
        mPosition = new Point();
        mButton = (Button) findViewById(R.id.button) ;
        mButtonSave = (Button) findViewById(R.id.button_save);
        mShare = false;

        inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);

        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE);
        }

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

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode){
                case (REQUEST_TAKE_PHOTO): {
                    setPic(); //TODO
                    break;
                }
                case (PICK_IMAGE_REQUEST): {
                    Uri selectedimg = data.getData();
                    try {
                        mFrameLayout.removeAllViews();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedimg);
                        mBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        ImageView imageView = new ImageView(this);
                        imageView.setImageBitmap(mBitmap);
                        mFrameLayout.addView(imageView);
                        Drawable drawable = imageView.getDrawable();
                        Rect rect = drawable.getBounds();

                        int width = mBitmap.getWidth();//rect.width();
                        int height = mBitmap.getHeight();//rect.height();
                        boolean scale = true;
                        Log.d("Activity resoult", "w B: " + width + "h B: " + height + " w R: " + mFrameLayout.getWidth() + " h w R: " + mFrameLayout.getHeight());
                        int size = startSize;
                        if (width < mFrameLayout.getWidth() || height < mFrameLayout.getHeight()) {
                            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(width, height);
                            mFrameLayout.setLayoutParams(lp);
                            scale = false;
                        }
                        else {
                            float przez = ((float)mFrameLayout.getWidth())/((float)width);
                            size = (int) ((float)startSize*przez);
                            Log.d("Main", "przez = " + przez);
                        }
//                        mFrameLayout.setX(mBitmap.getWidth());
//                        mFrameLayout.setY(mBitmap.getHeight());
                        //mImageView.setImageBitmap(mBitmap);
                        //Canvas mCanvas = new Canvas(mBitmap);
                        //setContentView(new MyDragView(this, startSize, bitmap));
                        Log.d("Main", "size  = " + size);
                        myDragView = new MyDragView(this, size,  mFrameLayout.getWidth()/(float)width, mFrameLayout.getHeight()/(float)height, scale);
                        mFrameLayout.addView(myDragView);
                        mButton.setVisibility(View.VISIBLE);
                        mButtonSave.setVisibility(View.GONE);
                        mShare = false;
                        //myRect.draw(mCanvas);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_new, menu);
        inflater.inflate(R.menu.menu_share, menu);

        MenuItem item = menu.findItem(R.id.menu_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        if (mShare){
            item.setVisible(true);
        }
        else {
            item.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_picture:
                Intent intent = new Intent();
                // Show only images, no videos or anything else
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                // Always show the chooser (if there are multiple options available)
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setShareIntent() {
        // BEGIN_INCLUDE(update_sap)
        if (mShareActionProvider != null) {
            // Get the currently selected item, and retrieve it's share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);

            String bitmapPath = MediaStore.Images.Media.insertImage(getContentResolver(), mBitmap,"title", null);
//            Uri bitmapUri = Uri.parse(bitmapPath);

            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, getImageUri());


            // Now update the ShareActionProvider with the new share intent
            mShareActionProvider.setShareIntent(shareIntent);
        }
        // END_INCLUDE(update_sap)
    }

    public Uri getImageUri() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), mBitmap, "Title", null);
        return Uri.parse(path);
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

    public void galleryAddPic(View view) {
        MediaStore.Images.Media.insertImage(getContentResolver(), mBitmap, "super res" , "picture after super resolution");
//        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//        File f = new File(mCurrentPhotoPath);
//        Uri contentUri = Uri.fromFile(f);
//        mediaScanIntent.setData(contentUri);
//        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = mFrameLayout.getWidth();
        int targetH = mFrameLayout.getHeight();

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
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(mBitmap);
        mFrameLayout.addView(imageView);
    }

    public void superRes(View view){

        mPosition = myDragView.getPosition();
        Log.d("tutaj ", mPosition.toString());

        int x1 = mPosition.x;
        int y1 = mPosition.y;

        if (x1 < 0 ){
            x1 = 0;
        }
        else if(x1 >= mBitmap.getWidth()){
            x1 = mBitmap.getWidth() - startSize - 1;
        }
        if (y1 < 0 ){
            y1 = 0;
        }
        else if(y1 >= mBitmap.getHeight()){
            y1 = mBitmap.getHeight() - startSize - 1;
        }
        Log.d("tutaj 2 ", "x " + x1 + " y1 " + y1);
        Log.d("tutaj 3 ", "h = " + mBitmap.getHeight() + " w = " + mBitmap.getWidth());
        Bitmap croppedBitmap = Bitmap.createBitmap(mBitmap, x1, y1, startSize, startSize);
        final Bitmap croppedBitmap2 = croppedBitmap.copy(Bitmap.Config.ARGB_8888, true);

        setPicture(croppedBitmap2);

        Log.d("tutaj 3 ", " " + croppedBitmap2.getHeight());
        croppedBitmap2.getPixels(intValues, 0, croppedBitmap2.getWidth(), 0, 0,
                croppedBitmap2.getWidth(), croppedBitmap2.getHeight());

        mButton.setVisibility(View.GONE);

        new Thread(new Runnable() {
            @Override
            public void run() {
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

                Bitmap newBitmap = Bitmap.createBitmap(desiredSize, desiredSize, Bitmap.Config.ARGB_8888);;

                newBitmap.setPixels(intValues2, 0, newBitmap.getWidth(), 0, 0, newBitmap.getWidth(), newBitmap.getHeight());
                mBitmap = newBitmap;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setPicture(mBitmap);
                        mButtonSave.setVisibility(View.VISIBLE);
                        setShareIntent();
                        mShare = true;
                        invalidateOptionsMenu();
                    }
                });
            }
        }).start();


        Log.d("koniec", "sam koniec");
    }

    private void setPicture(Bitmap bitmap){
        Log.d("tutaj ", " w setPicture ");
        mFrameLayout.removeAllViews();
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        mFrameLayout.addView(imageView);
        mFrameLayout.invalidate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mButtonSave.setText(R.string.save);
                    mButtonSave.setEnabled(true);
                }
                else {
                    mButtonSave.setText(R.string.no_save);
                    mButtonSave.setEnabled(false);
                }
        }
    }
}