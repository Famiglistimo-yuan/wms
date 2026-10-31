package com.wms.wmsserver.materialio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.wmsserver.materialio.entity.MaterialIoRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 子系统2 —— 出入库流水 Mapper。
 * <p>
 * 由于 {@code material_io_record} 表是子系统2 专属（旧子系统1 无此表），
 * 本类不存在 bean 名冲突，可以保持 {@code @Mapper} + BaseMapper 最简形式。
 */
@Mapper
public interface MaterialIoRecordMapper extends BaseMapper<MaterialIoRecord> {
}
