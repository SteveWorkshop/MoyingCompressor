package rachel.materialapps.imgcompressor.ui.pages;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rachel.materialapps.imgcompressor.R;
import rachel.materialapps.imgcompressor.databinding.FragmentFolderBinding;
import rachel.materialapps.imgcompressor.logic.utils.ImageCompressorUtil;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FolderFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FolderFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private FragmentFolderBinding binding;

    private FolderViewModel mViewModel;

    private Uri srcUri, dstUri;
    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /* SAF 启动器 */
    /* ===== SAF 启动器 ===== */
    private final ActivityResultLauncher<Intent> pickSrc = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    srcUri = result.getData().getData();
                    requireContext().getContentResolver()
                            .takePersistableUriPermission(srcUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    binding.tvSrc.setText("源：" + DocumentFile.fromTreeUri(requireContext(), srcUri).getName());
                }
            });

    private final ActivityResultLauncher<Intent> pickDst = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    dstUri = result.getData().getData();
                    requireContext().getContentResolver()
                            .takePersistableUriPermission(dstUri,
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    binding.tvDst.setText("目标：" + DocumentFile.fromTreeUri(requireContext(), dstUri).getName());
                }
            });

    public FolderFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FolderFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FolderFragment newInstance(String param1, String param2) {
        FolderFragment fragment = new FolderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentFolderBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        return view;
        // inflater.inflate(R.layout.fragment_folder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnPickSrc.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickSrc.launch(i);
        });
        binding.btnPickDst.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            pickDst.launch(i);
        });

        binding.btnStart.setOnClickListener(v -> {
            if (srcUri == null || dstUri == null) {
                Toast.makeText(getContext(), "请先选择目录", Toast.LENGTH_SHORT).show();
                return;
            }
            startCompress();
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this, new SavedStateViewModelFactory(getActivity().getApplication(), this)).get(FolderViewModel.class);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;          // 防内存泄漏
        threadPool.shutdown();
    }

    private void startCompress() {
        binding.progress.setVisibility(View.VISIBLE);
        binding.progress.setIndeterminate(true);
        threadPool.execute(() -> {
            try {
                DocumentFile srcDoc = DocumentFile.fromTreeUri(requireContext(), srcUri);
                DocumentFile dstDoc = DocumentFile.fromTreeUri(requireContext(), dstUri);
                ImageCompressorUtil.Options opt = buildOptionsFromUi();
                compressRecursive(srcDoc, dstDoc, opt);
                getActivity().runOnUiThread(() -> {
                    binding.progress.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "全部完成！", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    binding.progress.setVisibility(View.GONE);
                    binding.tvLog.setText("出错：" + e.getMessage());
                });
            }
        });
    }

    private ImageCompressorUtil.Options buildOptionsFromUi() {
        ImageCompressorUtil.Options o = new ImageCompressorUtil.Options();
        if (binding.rbShort.isChecked()) o.mode = ImageCompressorUtil.ScaleMode.FIXED_SHORT_EDGE;
        else if (binding.rbLong.isChecked()) o.mode = ImageCompressorUtil.ScaleMode.FIXED_LONG_EDGE;
        else o.mode = ImageCompressorUtil.ScaleMode.FIXED_SIZE;

        try { o.targetShortEdge = Integer.parseInt(binding.etSize.getText().toString()); } catch (Exception ignore) {}
        try { o.dpiX = o.dpiY = Integer.parseInt(binding.etDpi.getText().toString()); } catch (Exception ignore) {}
        try { o.quality = Integer.parseInt(binding.etQuality.getText().toString()); } catch (Exception ignore) {}
        return o;
    }

    private void compressRecursive(DocumentFile src, DocumentFile dst, ImageCompressorUtil.Options opt) throws IOException {
        for (DocumentFile f : src.listFiles()) {
            if (f.isDirectory()) {
                DocumentFile sub = dst.createDirectory(f.getName());
                compressRecursive(f, sub, opt);
            } else if (f.isFile() && f.getName() != null &&
                    f.getName().toLowerCase().matches(".+\\.(jpg|jpeg|png)")) {

                File tmpIn  = new File(requireContext().getCacheDir(), "in_"  + f.getName());
                File tmpOut = new File(requireContext().getCacheDir(), "out_" + f.getName());

                /* 拷贝到临时文件 */
                try (InputStream in = requireContext().getContentResolver().openInputStream(f.getUri());
                     OutputStream out = new FileOutputStream(tmpIn)) {
                    byte[] buf = new byte[8192]; int r;
                    while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
                }

                /* 压缩 */
                ImageCompressorUtil.compressImage(tmpIn, tmpOut, opt);

                /* 写回目标目录 */
                DocumentFile newFile = dst.createFile("image/jpeg", f.getName());
                try (OutputStream dstOut = requireContext().getContentResolver().openOutputStream(newFile.getUri());
                     InputStream tmpInStream = new FileInputStream(tmpOut)) {
                    byte[] buf = new byte[8192]; int r;
                    while ((r = tmpInStream.read(buf)) != -1) dstOut.write(buf, 0, r);
                }

                tmpIn.delete(); tmpOut.delete();
                getActivity().runOnUiThread(() -> binding.tvLog.setText("已处理：" + f.getName()));
            }
        }
    }
}