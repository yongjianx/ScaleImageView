package com.example.skyworthclub.imagescale;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class LauchActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lauch);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(new LauchAdapter());
    }


    public static class LauchAdapter extends RecyclerView.Adapter<LauchAdapter.ViewHolder>{
        private final String[] OPTIONS = {"Sample", "RecyclerView Sample"};

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final ViewHolder viewHolder = ViewHolder.newInstance(parent);

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Class clazz = null;
                    switch (viewHolder.getAdapterPosition()){
                        case 0:
                            clazz = SampleActivity.class;
                            break;

                        case 1:
                            clazz = ViewPagerActivity.class;
                            break;
                    }

                    Context context = viewHolder.itemView.getContext();
                    if (clazz != null)
                        context.startActivity(new Intent(context, clazz));
                }
            });
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.onBind(OPTIONS[position]);
        }

        @Override
        public int getItemCount() {
            return OPTIONS.length;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder{

            public TextView textView;

            private ViewHolder(View view){
                super(view);
                textView = view.findViewById(R.id.textView);
            }

            public static ViewHolder newInstance(ViewGroup parent){
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.lauch_item,
                        parent, false);
                return new ViewHolder(view);
            }

            public void onBind(String title){
                textView.setText(title);
            }
        }
    }

}
