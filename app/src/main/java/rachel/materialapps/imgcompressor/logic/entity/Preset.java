package rachel.materialapps.imgcompressor.logic.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rachel.materialapps.imgcompressor.logic.utils.ImageCompressorUtil;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Preset {
    public String name;          // 显示文字
    public ImageCompressorUtil.ScaleMode mode;
    public int size;             // 短边/长边/固定宽或高
    public int dpi;
    public int quality;
    public int width;
    public int height;

    @Override
    public String toString() {
        return name;
    }
}
