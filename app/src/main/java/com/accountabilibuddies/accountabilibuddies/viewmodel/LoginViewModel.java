package com.accountabilibuddies.accountabilibuddies.viewmodel;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.accountabilibuddies.accountabilibuddies.model.Category;
import com.accountabilibuddies.accountabilibuddies.model.Friend;
import com.accountabilibuddies.accountabilibuddies.network.APIClient;
import com.facebook.AccessToken;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoginViewModel {

    public static final String TAG = LoginViewModel.class.getSimpleName();

    private AppCompatActivity context;

    public interface LoggedInListener {

        void onSuccess();
        void onFailure();
    }

    public LoginViewModel(AppCompatActivity context) {

        this.context = context;
    }

    private void addCategoriesForNewUser(ParseUser user) {

        List<Category> categories = new ArrayList<>();
        user.put(Category.PLURAL, categories);
    }

    private void saveFriend(String facebookId, String name, String photoUrl) {

        Friend friend = new Friend();

        friend.setFacebookId(facebookId);
        friend.setName(name);
        friend.setPhotoUrl(photoUrl);
        friend.setFriendOfId(ParseUser.getCurrentUser().getObjectId());

        APIClient.getClient().createFriend(friend, new APIClient.CreateFriendListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Friend creation success!");
            }

            @Override
            public void onFailure(String errorMessage) {

                Log.d(TAG, "Friend creation failure!");
            }
        });
    }

    public void refreshToken(LoggedInListener listener) {

        AccessToken.refreshCurrentAccessTokenAsync(new AccessToken.AccessTokenRefreshCallback() {
            @Override
            public void OnTokenRefreshed(AccessToken accessToken) {
                listener.onSuccess();
            }

            @Override
            public void OnTokenRefreshFailed(FacebookException exception) {
                listener.onFailure();
            }
        });
    }

    public void createFriendsList() {

        GraphRequest friendRequest = GraphRequest.newMyFriendsRequest(
                AccessToken.getCurrentAccessToken(),

                (objects,  response) -> {
                    Log.d(TAG + "Friends List", response.toString());
                    JSONObject resultsJson = response.getJSONObject();

                    try {
                        JSONArray resultsArray = resultsJson.getJSONArray("data");

                        for (int i = 0; i < resultsArray.length(); i++) {

                            JSONObject user = resultsArray.getJSONObject(i);
                            JSONObject picture = user.getJSONObject("picture");
                            JSONObject pictureData = picture.getJSONObject("data");

                            saveFriend(
                                user.getString("id"),
                                user.getString("name"),
                                pictureData.getString("url")
                            );
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
        );

        Bundle params = new Bundle();
        params.putString("fields", "id, name, email, picture, cover");
        friendRequest.setParameters(params);
        friendRequest.executeAsync();
    }

    public void logIn(View view) {

        logInWithReadPermissions(new LoggedInListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Logged in!");
            }

            @Override
            public void onFailure() {
                Log.d(TAG, "User cancelled log in.");
            }
        });
    }

    public void logInWithReadPermissions(LoggedInListener listener) {

        ParseFacebookUtils.logInWithReadPermissionsInBackground(
            context,
            Arrays.asList("public_profile", "user_friends"),

            (ParseUser user, ParseException err) -> {
                if (user == null) {
                    Log.d(TAG, "Uh oh. The user cancelled the Facebook login.");
                    listener.onFailure();
                } else if (user.isNew()) {
                    Log.d(TAG, "User signed up and logged in through Facebook!");
                    addCategoriesForNewUser(user);
                    getProfileDataForUser(user);
                    listener.onSuccess();
                } else {
                    Log.d(TAG, "User logged in through Facebook!");
                    getProfileDataForUser(user);
                    listener.onSuccess();
                }
            }
        );
    }

    public void getProfileDataForUser(ParseUser user) {

        GraphRequest profileDataRequest = GraphRequest.newMeRequest(
            AccessToken.getCurrentAccessToken(),

            (JSONObject data, GraphResponse response) -> {

                Log.d(TAG + "profile data", response.toString());

                if (response.getError() != null) {
                    Log.d(TAG, "Profile data request failed");
                } else {
                    parseProfileData(user, data);
                }
            }
        );

        Bundle params = new Bundle();
        params.putString("fields", "id, name, email, picture, cover");
        profileDataRequest.setParameters(params);
        profileDataRequest.executeAsync();
    }

    private void parseProfileData(ParseUser user, JSONObject data) {

        try {
            String id = data.getString("id");
            String name = data.getString("name");
            String email = data.getString("email");

            JSONObject picture = data.getJSONObject("picture");
            JSONObject pictureData = picture.getJSONObject("data");
            String profilePhotoUrl = pictureData.getString("url");

            JSONObject cover = data.getJSONObject("cover");
            String coverPhotoUrl = cover.getString("source");

            user.setUsername(email);
            user.setEmail(email);

            user.put("facebookId", id);
            user.put("name", name);
            user.put("profilePhotoUrl", profilePhotoUrl);
            user.put("coverPhotoUrl", coverPhotoUrl);
            user.saveInBackground();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void getFriendsForCurrentUser() {

        APIClient.getClient().getFriendsByUserId(
            ParseUser.getCurrentUser().getObjectId(),
            new APIClient.GetFriendsListener() {

                @Override
                public void onSuccess(List<Friend> friends) {

                    Log.d(TAG, "Here are my friends: " + friends.toString());
                }

                @Override
                public void onFailure(String errorMessage) {

                    Log.d(TAG, "Error getting friends list.");
                }
            }
        );
    }
}
