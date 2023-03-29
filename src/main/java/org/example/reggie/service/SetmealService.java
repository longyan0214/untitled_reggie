package org.example.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.reggie.dto.SetmealDto;
import org.example.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDto
     */
    void saveWidthDish(SetmealDto setmealDto);


    /**
     * 删除套餐，同时需要删除套餐和菜品的关联关系
     * @param ids
     */
    void removeWithDish(List<Long> ids);
}
