package com.example.employeemanagementsystem;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "Department.findByName",
                query = "SELECT d FROM Department d WHERE LOWER(d.name) = LOWER(:name)"
        ),
        @NamedQuery(
                name = "Department.findWithEmployeesByName",
                query = "SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.employees " +
                        "WHERE LOWER(d.name) = LOWER(:name)"
        )
})
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
public class Department extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "department_seq")
    @SequenceGenerator(name = "department_seq", sequenceName = "department_seq", allocationSize = 25)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @JsonIgnore
    @OneToMany(mappedBy = "department")
    @BatchSize(size = 25)
    private List<Employee> employees = new ArrayList<>();
}
