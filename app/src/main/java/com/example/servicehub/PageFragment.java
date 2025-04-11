package com.example.servicehub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PageFragment extends Fragment {
    private static final String ARG_PAGE_NUMBER = "page_number";

    public static PageFragment newInstance(int pageNumber) {
        PageFragment fragment = new PageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE_NUMBER, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page, container, false);
        TextView textView = view.findViewById(R.id.textView);
        Button actionButton = view.findViewById(R.id.actionButton);

        int pageNumber = getArguments() != null ? getArguments().getInt(ARG_PAGE_NUMBER) : 1;
        textView.setText("Page " + pageNumber);

        // Unique actions for each page
        actionButton.setText("Action for Page " + pageNumber);
        actionButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "You clicked on Page " + pageNumber, Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}
