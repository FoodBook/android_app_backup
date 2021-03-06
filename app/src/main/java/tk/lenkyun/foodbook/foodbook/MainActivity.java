package tk.lenkyun.foodbook.foodbook;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.FacebookSdk;
import com.github.clans.fab.FloatingActionButton;

import tk.lenkyun.foodbook.foodbook.Client.Helper.Service.ProfileHelper;
import tk.lenkyun.foodbook.foodbook.Data.Content;
import tk.lenkyun.foodbook.foodbook.Data.FoodPost;
import tk.lenkyun.foodbook.foodbook.Data.NewsFeed;
import tk.lenkyun.foodbook.foodbook.Data.Photo.Photo;
import tk.lenkyun.foodbook.foodbook.Client.Helper.Interface.CameraHelper;
import tk.lenkyun.foodbook.foodbook.Client.Helper.Interface.GalleryHelper;
import tk.lenkyun.foodbook.foodbook.Client.Helper.Interface.Listener.ObjectListener;
import tk.lenkyun.foodbook.foodbook.Client.Helper.Repository;
import tk.lenkyun.foodbook.foodbook.Client.Helper.Service.FacebookHelper;
import tk.lenkyun.foodbook.foodbook.Client.Service.LoginService;
import tk.lenkyun.foodbook.foodbook.Client.Service.NewsFeedService;
import tk.lenkyun.foodbook.foodbook.Client.Service.Listener.ContentListener;
import tk.lenkyun.foodbook.foodbook.Client.Service.ContentService;
import tk.lenkyun.foodbook.foodbook.Data.User.Profile;
import tk.lenkyun.foodbook.foodbook.Data.User.User;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int LOGIN_ID = 1;
    private CameraHelper cameraHelper;
    private GalleryHelper galleryHelper;
    private NewsFeed newsFeed;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());

        uiCheckLogin();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        FloatingActionButton fabCamera = (FloatingActionButton) findViewById(R.id.fab_camera);

        cameraHelper = new CameraHelper(this);
        galleryHelper = new GalleryHelper(this);
        final ObjectListener<Photo> photoListener = new ObjectListener<Photo>() {
            @Override
            public void onTaken(Photo object, int extra) {
                Repository.getInstance().setData("UploadPhoto", object);
                Intent intent = new Intent(MainActivity.this, PhotoUploadActivity.class);
                intent.putExtra("orientation", extra);
                startActivityForResult(intent, 100);
            }
        };
        cameraHelper.registerListener(photoListener);
        galleryHelper.registerListener(photoListener);

        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraHelper.take();
            }
        });

        FloatingActionButton fabGallery = (FloatingActionButton) findViewById(R.id.fab_gallery);
        fabGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                galleryHelper.take();
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.newsfeed_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        PostAdapter postAdapter = new PostAdapter(newsFeed);

        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(postAdapter);
        updateNewsFeed();

        recyclerView.setHasFixedSize(true);
    }

    private class PostAdapter extends RecyclerView.Adapter<PostViewHolder>
    {
        NewsFeed newsFeed;
        int counter = 0;
        public PostAdapter(NewsFeed newsFeed){
            this.newsFeed = newsFeed;
            if(newsFeed != null){
                counter = newsFeed.countFoodPost();
            }
        }

        @Override
        public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.feed, parent, false);
            PostViewHolder pvh = new PostViewHolder(v);
            return pvh;
        }

        @Override
        public void onBindViewHolder(final PostViewHolder holder, int position) {
            NewsFeedService newsFeedService = NewsFeedService.getInstance();
            FoodPost foodPost = newsFeedService.getFeedIndex(newsFeed, position);
            if(foodPost == null){
                return;
            }
            final User owner = foodPost.getOwner();
            final Profile ownerProfile = owner.getProfile();

            holder.profileName.setText(ownerProfile.getFirstname() + "  " + ownerProfile.getLastname());
            holder.caption.setText(foodPost.getPostDetail().getCaption());

            ContentService.getInstance().getBitmap(
                    foodPost.getPostDetail().getPhoto(0),
                    new ContentListener<Bitmap>() {
                        @Override
                        public void onContentLoaded(final Content<Bitmap> content) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    holder.feedPhoto.setImageBitmap(content.getContent());
                                }
                            });
                        }

                        @Override
                        public void onContentFailed(String errorDetail) {
                        }
                    }
            );

            ProfileHelper profileHelper = new ProfileHelper(ownerProfile);
            profileHelper.getProfilePictureContent(
                    new ContentListener<Bitmap>() {
                        @Override
                        public void onContentLoaded(final Content<Bitmap> content) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    holder.profilePhoto.setImageBitmap(content.getContent());
                                }
                            });
                        }

                        @Override
                        public void onContentFailed(String errorDetail) {
                        }
                    }
            );
        }

        @Override
        public int getItemCount() {
            if(newsFeed != null) {
                return counter;
            }
            return 0;
        }
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        public TextView profileName, caption, date;
        public ImageView feedPhoto, profilePhoto;
        public PostViewHolder(View itemView){
            super(itemView);
            profileName = (TextView) itemView.findViewById(R.id.profile_name);
            caption = (TextView) itemView.findViewById(R.id.caption);

            feedPhoto = (ImageView) itemView.findViewById(R.id.foodbook_pic);
            profilePhoto = (ImageView) itemView.findViewById(R.id.profile_picture);
            date = (TextView) itemView.findViewById(R.id.date);
        }
    }


    public void updateNewsFeed(){
        newsFeed = NewsFeedService.getInstance().getNewsFeed();
        PostAdapter postAdapter = new PostAdapter(newsFeed);
        recyclerView.setAdapter(postAdapter);
    }

    public void uiCheckLogin(){
        if(!LoginService.getInstance().validateCurrentSession()) {
            Intent loginActivity = new Intent(this, LoginActivity.class);
            startActivityForResult(loginActivity, LOGIN_ID);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camara) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_logout) {
            FacebookHelper.getInstance().logout();
            uiCheckLogin();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        cameraHelper.onActivityResult(requestCode, resultCode, data);
        galleryHelper.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case LOGIN_ID:
                updateProfileUI();
                break;
            case 100:
                updateNewsFeed();
                break;
        }
    }

    private void updateProfileUI() {
        if(!LoginService.getInstance().validateCurrentSession()){
            return;
        }

        final ImageView profile = (ImageView) findViewById(R.id.profile_picture);
        final LinearLayout profileCover = (LinearLayout) findViewById(R.id.cover_layout);
        final Profile userProfile = LoginService.getInstance().getUser().getProfile();
        final Photo profilePicture = userProfile.getProfilePicture();

        final MainActivity self = this;

        ProfileHelper profileHelper = new ProfileHelper(userProfile);
        profileHelper.getProfilePictureContent(
            new ContentListener<Bitmap>() {
                @Override
                public void onContentLoaded(Content<Bitmap> content) {
                    final Bitmap contentIn = content.getContent();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            profile.setImageBitmap(
                                    Bitmap.createScaledBitmap(contentIn,
                                            getResources().getInteger(R.integer.profile_width),
                                            getResources().getInteger(R.integer.profile_height), false));
                        }
                    });
                }

                @Override
                public void onContentFailed(String errorDetail) {
                }
            }
        );

        profileHelper.getCoverPictureContent(
            new ContentListener<Bitmap>() {
                @Override
                public void onContentLoaded(Content<Bitmap> content) {
                    final Bitmap contentIn = content.getContent();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap scaled = Bitmap.createScaledBitmap(contentIn,
                                    200, 100, false);
                            Canvas canvas = new Canvas(scaled);
                            Paint paint = new Paint();
                            paint.setARGB(150, 0, 0, 0);
                            canvas.drawRect(0, 0, 200, 100, paint);

                            profileCover.setBackgroundDrawable(new BitmapDrawable(scaled));
                        }
                    });
                }

                @Override
                public void onContentFailed(String errorDetail) {

                }
            }
        );

        TextView userFullname = (TextView) findViewById(R.id.user_fullname);
        userFullname.setText(userProfile.getFirstname() + "  " + userProfile.getLastname());

        TextView userUsername = (TextView) findViewById(R.id.user_username);
        userUsername.setText(LoginService.getInstance().getUser().getUsername());

    }
}
