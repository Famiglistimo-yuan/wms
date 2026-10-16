package com.wms.wmsserver.material.controller;

import com.wms.common.Result;
import com.wms.wmsserver.auth.annotation.RequirePermission;
import com.wms.wmsserver.material.dto.MaterialCreateDTO;
import com.wms.wmsserver.material.dto.MaterialUpdateDTO;
import com.wms.wmsserver.material.dto.MaterialVO;
import com.wms.wmsserver.material.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 物料档案管理（FR-1-3）。
 * 权限码：menu.material.view / add / edit / delete。
 */
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService service;

    @GetMapping
    @RequirePermission("menu.material.view")
    public Result<List<MaterialVO>> list(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            return Result.success(service.search(keyword));
        }
        return Result.success(service.listAll());
    }

    @GetMapping("/{id}")
    @RequirePermission("menu.material.view")
    public Result<MaterialVO> get(@PathVariable Long id) {
        return Result.success(service.get(id));
    }

    @PostMapping
    @RequirePermission("menu.material.add")
    public Result<MaterialVO> create(@Valid @RequestBody MaterialCreateDTO dto) {
        return Result.success(service.create(dto));
    }

    @PutMapping("/{id}")
    @RequirePermission("menu.material.edit")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MaterialUpdateDTO dto) {
        service.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("menu.material.delete")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }
}
