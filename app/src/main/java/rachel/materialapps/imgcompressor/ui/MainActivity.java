package rachel.materialapps.imgcompressor.ui;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.navigation.NavigationBarView;

import rachel.materialapps.imgcompressor.R;
import rachel.materialapps.imgcompressor.databinding.ActivityMainBinding;
import lombok.Getter;
import lombok.Setter;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Getter
    @Setter
    private NavHostFragment navHostFragment;

    @Getter
    @Setter
    private NavController navController;

    private MainViewModel mViewModel;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mViewModel = new ViewModelProvider(this, new SavedStateViewModelFactory(getApplication(), this)).get(MainViewModel.class);
        navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.my_nav_host_fragment);
        navController = navHostFragment.getNavController();
        binding.navigationRail.setOnItemSelectedListener(item->{
            switch (item.getItemId()){
                case R.id.folder_window:{
                    navController.navigate(R.id.folderFragment);
                    break;
                }
                case R.id.camera_window:{
                    navController.navigate(R.id.cameraFragment);
                    break;
                }
                case R.id.settings_window:{
                    navController.navigate(R.id.settingsFragment);
                    break;
                }
                default:{
                    break;
                }
            }
            return true;
        });
    }
}