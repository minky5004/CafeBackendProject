package com.example.cafebackendproject.domain.menu.repository;

import com.example.cafebackendproject.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    // 메뉴명 중복 체크
    boolean existsByName(String name);

    // 판매 중인 메뉴만 조회
    List<Menu> findAllByIsAvailableTrue();
}