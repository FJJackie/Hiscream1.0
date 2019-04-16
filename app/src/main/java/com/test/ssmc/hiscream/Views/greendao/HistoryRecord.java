package com.test.ssmc.hiscream.Views.greendao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Property;
import org.greenrobot.greendao.annotation.Transient;

@Entity
public class HistoryRecord {

    @Id(autoincrement = true)
    private Long id;

    @NotNull
    @Property(nameInDb = "EMPLOYEE_NAME")
    private String name;

    private int salary;

    @Transient
    private int tempUsageCount;

    @Generated(hash = 2028949425)
    public HistoryRecord(Long id, @NotNull String name, int salary) {
        this.id = id;
        this.name = name;
        this.salary = salary;
    }

    @Generated(hash = 1096856789)
    public HistoryRecord() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSalary() {
        return this.salary;
    }

    public void setSalary(int salary) {
        this.salary = salary;
    }
}
