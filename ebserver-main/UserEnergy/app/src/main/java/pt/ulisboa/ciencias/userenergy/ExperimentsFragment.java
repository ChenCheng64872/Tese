package pt.ulisboa.ciencias.userenergy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ExperimentsFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_experiments, container, false);

        // Find RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.experiment_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Set adapter with mock data
        List<Project> projects = getMockProjects();
        ProjectAdapter adapter = new ProjectAdapter(projects);
        recyclerView.setAdapter(adapter);

        return view;
    }

    private List<Project> getMockProjects() {
        List<Project> projects = new ArrayList<>();
        projects.add(new Project("Flashlight", "Tests flashlight functionality in various scenarios."));
        projects.add(new Project("GoogleKeep", "Simulates user interactions with Google Keep."));
        projects.add(new Project("WhatsApp", "Experiments with messaging and multimedia sharing."));
        projects.add(new Project("YouTube", "Tests video playback and search functionality."));
        projects.add(new Project("Instagram", "Simulates user interactions, including photo sharing and stories."));
        projects.add(new Project("TikTok", "Tests video creation, playback, and user interaction features."));
        return projects;

    }
}
