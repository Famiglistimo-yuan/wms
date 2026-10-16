package com.wms.wmsserver.person.controller;

import com.wms.common.Result;
import com.wms.wmsserver.auth.annotation.RequirePermission;
import com.wms.wmsserver.person.dto.PersonCreateDTO;
import com.wms.wmsserver.person.dto.PersonUpdateDTO;
import com.wms.wmsserver.person.dto.PersonVO;
import com.wms.wmsserver.person.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 人员档案管理（FR-1-2）。
 * 权限码：menu.person.view / add / edit / delete。
 */
@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService service;

    /** 全部人员（下拉用 + 搜索框清空时回退） */
    @GetMapping
    @RequirePermission("menu.person.view")
    public Result<List<PersonVO>> list(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            return Result.success(service.searchBySingleChar(keyword));
        }
        return Result.success(service.listAll());
    }

    @GetMapping("/{id}")
    @RequirePermission("menu.person.view")
    public Result<PersonVO> get(@PathVariable Long id) {
        return Result.success(service.get(id));
    }

    @PostMapping
    @RequirePermission("menu.person.add")
    public Result<PersonVO> create(@Valid @RequestBody PersonCreateDTO dto) {
        return Result.success(service.create(dto));
    }

    @PutMapping("/{id}")
    @RequirePermission("menu.person.edit")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PersonUpdateDTO dto) {
        service.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("menu.person.delete")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }
}
