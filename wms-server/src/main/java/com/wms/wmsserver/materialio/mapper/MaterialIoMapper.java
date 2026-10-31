package com.wms.wmsserver.materialio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.wmsserver.materialio.entity.Material;
import org.apache.ibatis.annotations.Mapper;

/**
 * 子系统2 —— 物料 Mapper（MyBatis-Plus BaseMapper）。
 * <p>
 * ⚠️ 类名定为 {@code MaterialIoMapper} 而非 {@code MaterialMapper}，
 * 是为了避开旧子系统1 {@code com.wms.wmsserver.mapper.MaterialMapper}
 * 在 Spring 容器中的 bean 名冲突（两者默认都叫 {@code materialMapper}）。
 * 同时子系统2 包下保留了一个 {@code @Deprecated} 空接口 MaterialMapper，
 * 它未标注 {@code @Mapper}，不会被扫描为 bean。
 */
@Mapper
public interface MaterialIoMapper extends BaseMapper<Material> {
}
