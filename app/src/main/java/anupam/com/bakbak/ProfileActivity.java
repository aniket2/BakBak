package anupam.com.bakbak;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class    ProfileActivity extends AppCompatActivity {

    private ImageView mProfileImage;
    private TextView mProfileName, mProfileStatus, mProfileFriendCount;
    private Button mProfileSendReqBtn, mDeclineBtn;

    private DatabaseReference mUsersDatabase;
    private DatabaseReference mFriendReqDatabase;
    private DatabaseReference mFriendDatabase;
    private DatabaseReference mNotificationDatabase;

    private FirebaseUser mCurrent_user;

    private DatabaseReference mRootRef;

    private ProgressDialog mProgressDialog;

    private String mCurrent_state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String user_id = getIntent().getStringExtra("user_id");

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mFriendReqDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child("notification");

        mCurrent_user = FirebaseAuth.getInstance().getCurrentUser();

        mProfileImage = (ImageView)findViewById(R.id.profile_image);
        mProfileName = (TextView)findViewById(R.id.profile_displayName);
        mProfileStatus = (TextView)findViewById(R.id.profile_status);
        mProfileFriendCount = (TextView)findViewById(R.id.profile_totalFriends);
        mProfileSendReqBtn = (Button)findViewById(R.id.profile_send_req_btn);
        mDeclineBtn = (Button)findViewById(R.id.profile_deny_req_btn);

        mCurrent_state = "not_friends";

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading User Profile");
        mProgressDialog.setMessage("Please wait while we load user data.");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        mUsersDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String display_name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                mProfileName.setText(display_name);
                mProfileStatus.setText(status);
                Picasso.get().load(image).placeholder(R.drawable.default_avatar).into(mProfileImage);

                //------------------------- Friend list / Request Feature -------------------

                mFriendReqDatabase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if(dataSnapshot.hasChild(user_id)){

                            String req_type = dataSnapshot.child(user_id).child("request type").getValue().toString();

                            if(req_type.equals("received")){

                                mCurrent_state = "req_received";
                                mProfileSendReqBtn.setText("Accept Friend Request");

                                mDeclineBtn.setVisibility(View.VISIBLE);
                                mDeclineBtn.setEnabled(true);

                            }else if(req_type.equals("send")){

                                mCurrent_state = "req_send";
                                mProfileSendReqBtn.setText("Cancel Friend Request");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);

                            }

                            mProgressDialog.dismiss();

                        }else {

                            mFriendDatabase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    if(dataSnapshot.hasChild(user_id)){

                                        mCurrent_state = "friends";
                                        mProfileSendReqBtn.setText("Unfriend this person");

                                        mDeclineBtn.setVisibility(View.INVISIBLE);
                                        mDeclineBtn.setEnabled(false);

                                    }

                                    mProgressDialog.dismiss();

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                        }


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });



            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mProfileSendReqBtn.setEnabled(false);

                //--------------------------Not Friend State---------------------------------

                if (mCurrent_state.equals("not_friends")) {

                    DatabaseReference newNotificationref = mRootRef.child("notifications").child(user_id).push();
                    String newNotifactionId = newNotificationref.getKey();

                    HashMap<String, String> notificationData = new HashMap<>();
                    notificationData.put("from", mCurrent_user.getUid());
                    notificationData.put("type", "request");

                    Map requestMap = new HashMap();
                    requestMap.put("Friend_req/" + mCurrent_user.getUid() + "/" + user_id + "/request_type", "sent");
                    requestMap.put("Friend_req/" + user_id + "/" + mCurrent_user.getUid() + "/request_type", "received");
                    requestMap.put("notifications/" + user_id + "/" + newNotifactionId, notificationData);

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                            if (databaseError != null) {

                                Toast.makeText(ProfileActivity.this, "There was some error in sending request", Toast.LENGTH_SHORT).show();
                            }

                            mProfileSendReqBtn.setEnabled(true);

                            mCurrent_state = "req_sent";
                            mProfileSendReqBtn.setText("Cancel Friend Request");

                        }
                    });
                }



                //--------------------------Cancel Request State---------------------------------

                if(mCurrent_state.equals("req_send")){

                    mFriendReqDatabase.child(mCurrent_user.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            mFriendReqDatabase.child(user_id).child(mCurrent_user.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mProfileSendReqBtn.setEnabled(true);
                                    mCurrent_state = "not_friend";
                                    mProfileSendReqBtn.setText("Send Friend Request");

                                    mDeclineBtn.setVisibility(View.INVISIBLE);
                                    mDeclineBtn.setEnabled(false);

                                    Toast.makeText(ProfileActivity.this, "Request Cancelled", Toast.LENGTH_LONG).show();


                                }
                            });

                        }
                    });

                }


                // ---------------------Req Received State----------------

                if(mCurrent_state.equals("req_received")){

                   final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                   Map friendsMap = new HashMap();
                   friendsMap.put("Friends/" + mCurrent_user.getUid() + "/" + user_id + "/date", currentDate);
                   friendsMap.put("Friends/" + user_id + "/" + mCurrent_user.getUid() + "/date", currentDate);

                   friendsMap.put("Friend_req/" + mCurrent_user.getUid() + "/" + user_id, null);
                   friendsMap.put("Friends_req/" + user_id + "/" + mCurrent_user.getUid(), null);

                   mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                       @Override
                       public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                           if(databaseError == null){
                               mProfileSendReqBtn.setEnabled(true);
                               mCurrent_state = "friends";
                               mProfileSendReqBtn.setText("Unfriend this person");

                               mDeclineBtn.setVisibility(View.INVISIBLE);
                               mDeclineBtn.setEnabled(false);

                           } else {
                               String error = databaseError.getMessage();

                               Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                           }
                       }
                   });

                }

                //----------------unfriends------

                if(mCurrent_state.equals("friends")){

                    Map unfriendMap = new HashMap();
                    unfriendMap.put("Friends/" + mCurrent_user.getUid() + "/" + user_id, null);
                    unfriendMap.put("Friends/" + user_id + "/" + mCurrent_user.getUid(), null);

                    mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if(databaseError == null){
                                mProfileSendReqBtn.setEnabled(true);
                                mCurrent_state = "not_friends";
                                mProfileSendReqBtn.setText("Send Friend Request");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);

                            } else {
                                String error = databaseError.getMessage();

                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }
            }
        });

    }
}
