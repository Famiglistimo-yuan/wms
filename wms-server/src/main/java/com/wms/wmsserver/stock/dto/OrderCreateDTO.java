package com.wms.wmsserver.stock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 提交进出仓单请求（FR-2 / FR-3：单物料=明细长度 1，多物料=长度 N，同一入口）。
 * 进/出共用本 DTO，orderType 区分；操作人取当前登录用户，不由客户端传入。
 */
@Data
public class OrderCreateDTO {

    /** IN=进仓 OUT=出仓 */
    @NotBlank(message = "orderType 不能为空")
    @Pattern(regexp = "IN|OUT", message = "orderType 必须为 IN（进仓）或 OUT（出仓）")
    private String orderType;

    /** 单据日期 yyyy-MM-dd（SP 内按日期分片生成单号流水） */
    @NotBlank(message = "单据日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "单据日期格式须为 yyyy-MM-dd")
    private String orderDate;

    /** 经手人（人员档案 id，界面下拉选择） */
    @NotNull(message = "经手人不能为空")
    private Long handlerId;

    /** 整单备注（可空） */
    @Size(max = 500, message = "备注最长 500 字")
    private String remark;

    /** 明细数组，长度 ≥ 1；同单同物料重复由 SP 驳回（界面应合并重复行） */
    @NotEmpty(message = "单据明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "materialId 不能为空")
        private Long materialId;

        @NotNull(message = "数量不能为空")
        @Positive(message = "数量必须大于 0")
        private BigDecimal quantity;
    }
}
