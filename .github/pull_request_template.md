## Summary

<!-- 这个 PR 做了什么、为什么 -->

## Related

<!-- 关联 Issue："Closes #编号" 合并时自动关闭，仅关联用 "Relates to #编号"；无则删除本节 -->

## Testing

<!-- 如何验证的：跑过的命令、手动测试步骤、测试结果；无则说明原因 -->

## Screenshots

<!-- JavaFX 界面有改动时必须附截图；无界面变更删除本节 -->

## Checklist

- [ ] 提交信息符合 Conventional Commits（CONTRIBUTING §1）
- [ ] `mvn -DskipTests clean compile` 本地通过
- [ ] 新增/修改的接口与 TECHNICAL_DESIGN §6.2 清单一致，未私造路径或错误码
- [ ] 暂存区无敏感信息（application-local.yaml / .workbuddy/ / HANDOFF.md 已排除）
- [ ] 表结构变更已同步 docs/sql/schema.sql（如有）
- [ ] 文档已随行为变更更新（如有）
