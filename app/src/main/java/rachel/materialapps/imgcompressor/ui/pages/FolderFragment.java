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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rachel.materialapps.imgcompressor.R;
import rachel.materialapps.imgcompressor.databinding.FragmentFolderBinding;
import rachel.materialapps.imgcompressor.logic.entity.Preset;
import rachel.materialapps.imgcompressor.logic.utils.ImageCompressorUtil;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FolderFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FolderFragment extends Fragment {

    private static final List<Preset> PRESETS = Arrays.asList(
            new Preset("考研报名（短边800，dpi72，quality90）",
                    ImageCompressorUtil.ScaleMode.FIXED_SHORT_EDGE, 800, 72, 90,0,0),
            new Preset("公务员报名（短边600，dpi72，quality85）",
                    ImageCompressorUtil.ScaleMode.FIXED_SHORT_EDGE, 600, 72, 85,0,0),
            new Preset("教师资格证（短边480，dpi72，quality80）",
                    ImageCompressorUtil.ScaleMode.FIXED_SHORT_EDGE, 480, 72, 80,0,0),
            new Preset("四六级报名（短边500，dpi72，quality85）",
                    ImageCompressorUtil.ScaleMode.FIXED_SHORT_EDGE, 500, 72, 85,0,0),
            new Preset("一寸证件照（固定295×413，dpi300，quality95）",
                    ImageCompressorUtil.ScaleMode.FIXED_SIZE, 413, 300, 95,0,0),
            new Preset("打印高清（短边1600，dpi300，quality90）",
                    ImageCompressorUtil.ScaleMode.FIXED_SHORT_EDGE, 1600, 300, 90,0,0),
            new Preset("自定义", null, 0, 0, 0,0,0)
    );



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

        initSpinner();
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

        /* 监听模式切换，实时更新 hint 与启用状态 */
        RadioGroup.OnCheckedChangeListener modeListener = (group, checkedId) -> {
            boolean isFixed = checkedId == R.id.rbFixed;
            boolean isShort = checkedId == R.id.rbShort;
            boolean isLong  = checkedId == R.id.rbLong;

            // 输入框启用/禁用
            binding.etSize.setEnabled(true);   // 主框始终可改（模板回显时已被禁用）
            binding.etHeight.setEnabled(isFixed);

            // 文字提示一目了然
            if (isFixed) {
                binding.etSize.setHint("目标宽度，如 295");
                binding.etHeight.setHint("目标高度，如 413");
            } else if (isShort) {
                binding.etSize.setHint("短边像素值，如 800");
                binding.etHeight.setHint("短边模式无需填写");
            } else {   // 长边
                binding.etSize.setHint("长边像素值，如 1920");
                binding.etHeight.setHint("长边模式无需填写");
            }
        };
        binding.rgMode.setOnCheckedChangeListener(modeListener);

        /* 初始触发一次，保证进入页面就正确 */
        modeListener.onCheckedChanged(binding.rgMode, binding.rgMode.getCheckedRadioButtonId());
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

    private void initSpinner() {
        ArrayAdapter<Preset> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                PRESETS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPreset.setAdapter(adapter);

        binding.spinnerPreset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Preset p = PRESETS.get(position);
                boolean isCustom = "自定义".equals(p.name);

                /* 启用/禁用输入框（不清空内容） */
                binding.etSize.setEnabled(isCustom);
                binding.etDpi.setEnabled(isCustom);
                binding.etQuality.setEnabled(isCustom);
                /* 单选按钮也同步禁用/启用 */
                binding.rbShort.setEnabled(isCustom);
                binding.rbLong.setEnabled(isCustom);
                binding.rbFixed.setEnabled(isCustom);

                binding.etHeight.setEnabled(isCustom); // 一起控制

                if (isCustom) {
                    binding.tvLog.setText("已切换到自定义，可手动输入参数");
                    return;   // 不再回写模板值
                }

                /* 否则按模板回显（禁用状态下也会自动设值，但用户改不了） */
                if (p.mode == ImageCompressorUtil.ScaleMode.FIXED_SIZE) {
                    binding.rbFixed.setChecked(true);
                    binding.etSize.setText(String.valueOf(p.width));
                    binding.etHeight.setText(String.valueOf(p.height)); // 高单独写
                } else if (p.mode == ImageCompressorUtil.ScaleMode.FIXED_SHORT_EDGE) {
                    binding.rbShort.setChecked(true);
                    binding.etSize.setText(String.valueOf(p.size));
                } else {
                    binding.rbLong.setChecked(true);
                    binding.etSize.setText(String.valueOf(p.size));
                }
                binding.etDpi.setText(String.valueOf(p.dpi));
                binding.etQuality.setText(String.valueOf(p.quality));
                binding.tvLog.setText("已选择：" + p.name);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
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

        /* 1. 模式 */
        if (binding.rbFixed.isChecked()) {
            o.mode = ImageCompressorUtil.ScaleMode.FIXED_SIZE;
        } else if (binding.rbShort.isChecked()) {
            o.mode = ImageCompressorUtil.ScaleMode.FIXED_SHORT_EDGE;
        } else {
            o.mode = ImageCompressorUtil.ScaleMode.FIXED_LONG_EDGE;
        }

        /* 2. 尺寸 & 质量 & DPI 统一读值，空则给默认值 */
        try {
            if (o.mode == ImageCompressorUtil.ScaleMode.FIXED_SIZE) {
                // 固定模式：同时读宽、高
                o.targetWidth  = Integer.parseInt(binding.etSize.getText().toString().trim());
                o.targetHeight = Integer.parseInt(binding.etHeight.getText().toString().trim());
            } else {
                // 短边/长边模式：只读一个框
                int val = Integer.parseInt(binding.etSize.getText().toString().trim());
                if (o.mode == ImageCompressorUtil.ScaleMode.FIXED_SHORT_EDGE) {
                    o.targetShortEdge = val;
                } else {
                    o.targetLongEdge = val;
                }
            }
        } catch (NumberFormatException e) {
            // 用户没填或填错，给个体面的默认值避免崩溃
            if (o.mode == ImageCompressorUtil.ScaleMode.FIXED_SIZE) {
                o.targetWidth = 295;
                o.targetHeight = 413;
            } else {
                o.targetShortEdge = 800;   // 短边默认
                o.targetLongEdge  = 800;   // 长边默认（后面只用得到其中一个）
            }
        }

        /* 3. DPI */
        try {
            o.dpiX = o.dpiY = Integer.parseInt(binding.etDpi.getText().toString().trim());
        } catch (NumberFormatException e) {
            o.dpiX = o.dpiY = 72;   // 默认 72
        }

        /* 4. 质量 */
        try {
            o.quality = Integer.parseInt(binding.etQuality.getText().toString().trim());
        } catch (NumberFormatException e) {
            o.quality = 90;   // 默认 90
        }

        return o;
    }

    private void compressRecursive(DocumentFile src, DocumentFile dst, ImageCompressorUtil.Options opt) throws IOException {
        for (DocumentFile f : src.listFiles()) {
            if (f.isDirectory()) {
                DocumentFile sub = dst.createDirectory(f.getName());
                compressRecursive(f, sub, opt);
            } else if (f.isFile() && f.getName() != null &&
                    f.getName().toLowerCase().matches(".+\\.(jpg|jpeg|png)")) {

                File tmpIn = new File(requireContext().getCacheDir(), "in_" + f.getName());
                File tmpOut = new File(requireContext().getCacheDir(), "out_" + f.getName());

                /* 拷贝到临时文件 */
                try (InputStream in = requireContext().getContentResolver().openInputStream(f.getUri());
                     OutputStream out = new FileOutputStream(tmpIn)) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
                }

                /* 压缩 */
                ImageCompressorUtil.compressImage(tmpIn, tmpOut, opt);

                /* 写回目标目录 */
                DocumentFile newFile = dst.createFile("image/jpeg", f.getName());
                try (OutputStream dstOut = requireContext().getContentResolver().openOutputStream(newFile.getUri());
                     InputStream tmpInStream = new FileInputStream(tmpOut)) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = tmpInStream.read(buf)) != -1) dstOut.write(buf, 0, r);
                }

                tmpIn.delete();
                tmpOut.delete();
                getActivity().runOnUiThread(() -> binding.tvLog.setText("已处理：" + f.getName()));
            }
        }
    }
}