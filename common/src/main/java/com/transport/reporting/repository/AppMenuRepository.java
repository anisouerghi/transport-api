package com.transport.reporting.repository;

import com.transport.reporting.entity.AppMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppMenuRepository extends JpaRepository<AppMenu, Long> {

    List<AppMenu> findByActiveTrueOrderByDisplayOrderAsc();
}
