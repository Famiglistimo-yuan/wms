package com.wms.wmsserver.materialio.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 子系统2 —— Mapper 扫描配置。
 * <p>
 * 为什么单独写这个配置类而不是直接改启动类 {@code WmsServerApplication}：
 * <ol>
 *   <li>启动类原始写法只扫旧包 {@code com.wms.wmsserver.mapper}，
 *       把新包并进去会牵扯旧子系统，违反"子系统2 只改自己"原则</li>
 *   <li>即使并进去，{@code MaterialIoMapperConfig} 的
 *       {@code annotationClass = Mapper.class} 限定也是必须的 ——
 *       新包里有一个 {@code @Deprecated} 的 {@code MaterialMapper} 空接口
 *       （无 {@code @Mapper}），如果扫全部接口它也会被注册成 bean，
 *       跟旧子系统的 {@code com.wms.wmsserver.mapper.MaterialMapper} 重名冲突</li>
 * </ol>
 * 所以本配置类的 {@code annotationClass} 是避免冲突的关键。
 */
@Configuration
@MapperScan(basePackages = "com.wms.wmsserver.materialio.mapper", annotationClass = org.apache.ibatis.annotations.Mapper.class)
public class MaterialIoMapperConfig {
}
