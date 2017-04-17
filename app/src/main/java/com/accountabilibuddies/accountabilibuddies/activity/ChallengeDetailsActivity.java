package com.accountabilibuddies.accountabilibuddies.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.accountabilibuddies.accountabilibuddies.R;
import com.accountabilibuddies.accountabilibuddies.adapter.PostAdapter;
import com.accountabilibuddies.accountabilibuddies.application.ParseApplication;
import com.accountabilibuddies.accountabilibuddies.databinding.ActivityChallengeDetailsBinding;
import com.accountabilibuddies.accountabilibuddies.fragments.AddFriendsFragment;
import com.accountabilibuddies.accountabilibuddies.fragments.ChallengeMembersFragment;
import com.accountabilibuddies.accountabilibuddies.fragments.PostTextFragment;
import com.accountabilibuddies.accountabilibuddies.model.Challenge;
import com.accountabilibuddies.accountabilibuddies.model.Post;
import com.accountabilibuddies.accountabilibuddies.network.APIClient;
import com.accountabilibuddies.accountabilibuddies.util.CameraUtils;
import com.accountabilibuddies.accountabilibuddies.util.Constants;
import com.accountabilibuddies.accountabilibuddies.util.VideoUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChallengeDetailsActivity extends AppCompatActivity
        implements PostTextFragment.PostTextListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private ActivityChallengeDetailsBinding binding;
    APIClient client;
    protected ArrayList<Post> mPostList;
    protected PostAdapter mAdapter;
    protected LinearLayoutManager mLayoutManager;
    private GoogleApiClient mGoogleApiClient;
    private Challenge challenge;
    private Double mLatitude;
    private Double mLongitude;
    private String mAddress;

    private static final int PHOTO_INTENT_REQUEST = 100;
    private static final int REQUEST_LOCATION = 1;
    private static final int REQUEST_CAMERA = 2;
    private static final int GALLERY_INTENT_REQUEST = 200;
    private static final int VIDEO_INTENT_REQUEST = 300;
    private String mImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_challenge_details);

        challenge = ParseObject.createWithoutData(Challenge.class,
                getIntent().getStringExtra("challengeId"));

        //Setting toolbar
        setSupportActionBar(binding.toolbar);

        // Display icon in the toolbar
        getSupportActionBar().setTitle(getIntent().getStringExtra("name"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //Client instance
        client = APIClient.getClient();

        //Google client for location
        setupGoogleClient();

        mPostList = new ArrayList<>();
        mAdapter = new PostAdapter(this, mPostList);
        binding.rVPosts.setAdapter(mAdapter);
        binding.rVPosts.setItemAnimator(new DefaultItemAnimator());

        //Recylerview decorater
        RecyclerView.ItemDecoration itemDecoration =
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        binding.rVPosts.addItemDecoration(itemDecoration);

        mLayoutManager = new LinearLayoutManager(this);
        binding.rVPosts.setLayoutManager(mLayoutManager);

        //Swipe to refresh
        binding.swipeContainer.setOnRefreshListener(() -> getPosts());
        getPosts();
        setUpFriendsView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return true;
    }

    private void setUpFriendsView() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.flAddFriends, AddFriendsFragment.newInstance(challenge.getObjectId()));
        ft.commit();
    }

    private void setupGoogleClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    private void getPosts() {
        client.getPostList(challenge.getObjectId(), new APIClient.GetPostListListener(){
            @Override
            public void onSuccess(List<Post> postList) {
                if (postList != null) {
                    mPostList.clear();
                    mPostList.addAll(postList);
                    mAdapter.notifyDataSetChanged();
                }
                binding.swipeContainer.setRefreshing(false);
            }

            @Override
            public void onFailure(String error_message) {
                binding.swipeContainer.setRefreshing(false);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_exit:
                exitChallenge();
                return true;

            case R.id.action_list:
                showChallengeMembers();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showChallengeMembers() {
        ChallengeMembersFragment fragment = ChallengeMembersFragment.getInstance(challenge.getObjectId());
        fragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_FullScreen);
        fragment.show(getSupportFragmentManager(), "");
    }

    private void closeFabMenu(){
        binding.fabMenu.close(true);
    }

    public void sharePhotos(View view) {
        closeFabMenu();
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(intent, GALLERY_INTENT_REQUEST);
    }

    public void launchCamera(View view) {
        closeFabMenu();

        if (CameraUtils.cameraPermissionsGranted(this)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA);
        } else {
            launchCameraWithPermissionGranted();
        }
    }

    private void startRecording() {

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            File imageFile = null;
            try {
                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                imageFile = VideoUtils.createVideoFile(storageDir);
            } catch (IOException ex) {
                //TODO: Handle error
            }
            if (imageFile != null) {
                mImagePath = imageFile.getAbsolutePath();
                Uri videoUri = FileProvider.getUriForFile(this,
                        "com.accountabilibuddies.accountabilibuddies.fileprovider",
                        imageFile);
                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
                startActivityForResult(intent, VIDEO_INTENT_REQUEST);
            }
        } else {
            Toast.makeText(this, "No camera on this device", Toast.LENGTH_LONG).show();
        }
    }

    public void launchCameraForVideo(View view) {
        closeFabMenu();
        startRecording();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case PHOTO_INTENT_REQUEST: {
                if (resultCode == RESULT_OK) {
                    if (mImagePath == null) {
                        return;
                    }

                    binding.progressBarContainer.setVisibility(View.VISIBLE);
                    binding.avi.show();
                    //TODO: Need to optimize this scale to make image size more efficient
                    // wrt to the image view size
                    Bitmap bitmap = CameraUtils.scaleToFill(BitmapFactory.decodeFile(mImagePath)
                            , 800, 600);

                    client.uploadFile("post_image.jpg", bitmap, new APIClient.UploadFileListener() {
                        @Override
                        public void onSuccess(String fileLocation) {
                            Post post = new Post();
                            post.setType(Constants.TYPE_IMAGE);
                            post.setImageUrl(fileLocation);
                            List<ParseUser> users = new ArrayList<>();
                            post.setLikeList(users);
                            post.setOwner(ParseApplication.getCurrentUser());

                            APIClient.getClient().createPost(post, challenge.getObjectId(),
                                    new APIClient.PostListener() {
                                        @Override
                                        public void onSuccess() {
                                            Toast.makeText(ChallengeDetailsActivity.this,
                                                    "Creating post", Toast.LENGTH_LONG).show();
                                            onCreatePost(post);
                                        }

                                        @Override
                                        public void onFailure(String error_message) {
                                            Toast.makeText(ChallengeDetailsActivity.this,
                                                    "Error creating post", Toast.LENGTH_LONG).show();
                                            binding.avi.hide();
                                            binding.progressBarContainer.setVisibility(View.GONE);
                                        }
                                    });
                        }

                        @Override
                        public void onFailure(String error_message) {
                            binding.avi.hide();
                            binding.progressBarContainer.setVisibility(View.GONE);
                        }
                    });
                }
            }
            break;

            case GALLERY_INTENT_REQUEST: {
                if (resultCode == RESULT_OK) {
                    if (data.getData() == null) {
                        return;
                    }

                    binding.progressBarContainer.setVisibility(View.VISIBLE);
                    binding.avi.show();

                    Uri uri = data.getData();
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    } catch (IOException e) {
                        e.printStackTrace();
                        binding.avi.hide();
                        binding.progressBarContainer.setVisibility(View.GONE);
                        return;
                    }

                    client.uploadFile("post_image.jpg",bitmap, new APIClient.UploadFileListener() {
                        @Override
                        public void onSuccess(String fileLocation) {
                            Post post = new Post();
                            post.setType(Constants.TYPE_IMAGE);
                            post.setImageUrl(fileLocation);
                            List<ParseUser> users = new ArrayList<>();
                            post.setLikeList(users);
                            post.setOwner(ParseApplication.getCurrentUser());

                            Log.d("Objectid", challenge.getObjectId());

                            //TODO: Move the listener out of this function
                            APIClient.getClient().createPost(post, challenge.getObjectId(),
                                    new APIClient.PostListener() {
                                        @Override
                                        public void onSuccess() {
                                            Toast.makeText(ChallengeDetailsActivity.this,
                                                    "Creating post", Toast.LENGTH_LONG).show();
                                            onCreatePost(post);
                                        }

                                        @Override
                                        public void onFailure(String error_message) {
                                            Toast.makeText(ChallengeDetailsActivity.this,
                                                    "Error creating post", Toast.LENGTH_LONG).show();
                                            binding.avi.hide();
                                            binding.progressBarContainer.setVisibility(View.GONE);
                                        }
                                    });
                        }

                        @Override
                        public void onFailure(String error_message) {
                            binding.avi.hide();
                            binding.progressBarContainer.setVisibility(View.GONE);
                        }
                    });
                }

            }
            break;

            case VIDEO_INTENT_REQUEST: {
                if (resultCode == RESULT_OK) {
                    if (mImagePath == null) {
                        return;
                    }

                    binding.progressBarContainer.setVisibility(View.VISIBLE);
                    binding.avi.show();

                    File f = new File(mImagePath);

                    byte[] videoUp = new byte[0];
                    try {
                        videoUp = IOUtils.toByteArray( new FileInputStream(f));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    ParseFile file = new ParseFile("testVideo1", videoUp);

                    file.saveInBackground(new SaveCallback() {
                        public void done(ParseException e) {
                        }
                    });
                }
            }
            break;
        }
    }

    /**
     * Function to launch a dialog fragment to post text
     * @param view
     */
    public void launchTextPost(View view) {

        closeFabMenu();

        FragmentManager fm = getSupportFragmentManager();
        PostTextFragment fragment = PostTextFragment.getInstance(challenge.getObjectId());
        fragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_FullScreen);
        fragment.show(fm, "post_text");
    }

    /**
     *  Function to add post to the posts list.
     *  //TODO: Change this based on in what order the list is to be shown
     *
     */
    void onCreatePost(Post post) {
        mPostList.add(post);
        mAdapter.notifyDataSetChanged();
        mLayoutManager.scrollToPosition(mPostList.size() - 1);
        binding.avi.hide();
        binding.progressBarContainer.setVisibility(View.GONE);
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onFinishPost(Post post) {
        onCreatePost(post);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                    REQUEST_LOCATION);
        } else {
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (mLastLocation != null) {
                mLatitude = mLastLocation.getLatitude();
                mLongitude = mLastLocation.getLongitude();
                Geocoder gc = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = null;
                try {
                    addresses = gc.getFromLocation(mLatitude, mLongitude, 2);
                    if (addresses.size() > 0) {
                        mAddress = String.format("%s %s", addresses.get(0).getAddressLine(0),
                                addresses.get(0).getAddressLine(1));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @SuppressWarnings("all")
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if(grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
            } else {
                // Permission was denied or request was cancelled
            }
        }
        else if(requestCode == REQUEST_CAMERA) {
            if(grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCameraWithPermissionGranted();
            } else {
                Toast.makeText(this, "Cannot post a photo without permissions",Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    public void shareLocation(View view) {
        closeFabMenu();
        //ParseGeoPoint point = new ParseGeoPoint(mLatitude, mLongitude);

        Post post = new Post();
        post.setType(Constants.TYPE_LOCATION);
        List<ParseUser> users = new ArrayList<>();
        post.setLikeList(users);
        post.setLatitude(mLatitude);
        post.setLongitude(mLongitude);
        post.setAddress(mAddress);
        post.setOwner(ParseApplication.getCurrentUser());

        APIClient.getClient().createPost(post, challenge.getObjectId(),
            new APIClient.PostListener() {

                @Override
                public void onSuccess() {
                    Toast.makeText(ChallengeDetailsActivity.this,
                            "Creating post", Toast.LENGTH_LONG).show();
                    onCreatePost(post);
                }

                @Override
                public void onFailure(String error_message) {
                    Toast.makeText(ChallengeDetailsActivity.this,
                            "Error creating post", Toast.LENGTH_LONG).show();
                }
            }
        );
    }
    /**
     * Function create an image file to be used by the camera app to store the
     * image. Intent is then dispatched after the local image path is stored
     * which later will be used to upload the file
     * @param intent
     */
    private void dispatchTakePictureIntent(Intent intent) {

        File imageFile = null;
        try {
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            imageFile = CameraUtils.createImageFile(storageDir);
        } catch (IOException ex) {
            //TODO: Handle error
        }
        // Continue only if the File was successfully created
        if (imageFile != null) {
            //This path will be used to save in the post
            mImagePath = imageFile.getAbsolutePath();
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.accountabilibuddies.accountabilibuddies.fileprovider",
                    imageFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(intent, PHOTO_INTENT_REQUEST);
        }
    }

    private void exitChallenge() {

        challenge.fetchIfNeededInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (challenge.getOwner().getObjectId().equals(ParseUser.getCurrentUser().getObjectId())) {
                    confirmDeletion();
                } else {
                    client.exitChallenge(challenge.getObjectId(), new APIClient.ChallengeListener() {
                        @Override
                        public void onSuccess() {
                            Snackbar.make(binding.cLayout, "Exit successful", Snackbar.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(String error_message) { }
                    });
                }
            }
        });
    }

    private void confirmDeletion() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setTitle("Confirm Delete...");
        alertDialog.setMessage("You are the owner of this challenge. Are you sure you want delete this?");
        //alertDialog.setIcon(R.drawable.delete);

        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                client.deleteChallenge(challenge.getObjectId(), new APIClient.ChallengeListener() {
                    @Override
                    public void onSuccess() {
                        Snackbar.make(binding.cLayout, "Delete successful", Snackbar.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onFailure(String error_message) {

                    }
                });
            }
        });

        // Setting Negative "NO" Button
        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,	int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, DrawerActivity.class);
        intent.putExtra("exit", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        super.onBackPressed();
    }

    private void launchCameraWithPermissionGranted() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            dispatchTakePictureIntent(intent);
        }
    }
}
