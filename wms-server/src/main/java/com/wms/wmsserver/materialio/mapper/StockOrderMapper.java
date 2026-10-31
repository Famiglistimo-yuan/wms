package com.wms.wmsserver.materialio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.wmsserver.materialio.entity.StockOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockOrderMapper extends BaseMapper<StockOrder> {
}
