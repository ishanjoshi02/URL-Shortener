package com.example.ishan.urlshortener;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.freesoulapps.preview.android.Preview;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private List<String> mDataset;
    private RecyclerView mRecyclerView;
    private MyAdapter myAdapter;
    static final String TAG = "URLShortenerMain";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDataset = new ArrayList<>();
        mAuth = FirebaseAuth.getInstance();
        startService(new Intent(this, ClipboardMonitorService.class));
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null)
            startActivity(new Intent(this, LoginActivity.class));
        FirebaseDatabase
                .getInstance().getReference()
                .child("users")
                .child(currentUser.getUid())
                .addValueEventListener(mValueEventListener);
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
    }
    ValueEventListener mValueEventListener =
            new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    int dataSize = mDataset.size();
                    if (dataSize >= 1) {
                        for(String url : mDataset)
                            mDataset.remove(url);
                        myAdapter.notifyDataSetChanged();
                    }

                    for (DataSnapshot dsp : dataSnapshot.getChildren()) {
                        mDataset.add(dsp.getValue().toString());
                        Log.d(TAG, "Got Link " + dsp.getValue().toString());
                    }
                    myAdapter = new MyAdapter(mDataset);
                    myAdapter.notifyDataSetChanged();
                    mRecyclerView.setAdapter(myAdapter);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {}
            };

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<String> mDataset;
        MyAdapter(List<String> mDataset) {
            Log.d(TAG, "Constructor Called");
            this.mDataset = mDataset;
        }
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(
                    LayoutInflater
                            .from(parent.getContext())
                            .inflate(R.layout.my_card_view, parent, false)
            );
        }
        @Override
        public void onBindViewHolder(MyAdapter.ViewHolder holder, int position) {
            String url = mDataset.get(position);
            holder.preview.setData(url);
            holder.linkText.setText(url);
        }
        @Override
        public int getItemCount() {return mDataset.size();}
        public class ViewHolder extends RecyclerView.ViewHolder implements Preview.PreviewListener {
            Preview preview;
            TextView linkText;
            public ViewHolder(View itemView) {
                super(itemView);
                preview = (Preview) itemView.findViewById(R.id.preview);
                preview.setListener(this);
                linkText = (TextView) itemView.findViewById(R.id.link);
            }

            @Override
            public void onDataReady(Preview pv) {
                this.preview.setMessage(pv.getLink());
            }
        }
    }
}