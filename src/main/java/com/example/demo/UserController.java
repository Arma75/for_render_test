package com.example.demo;

import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class UserController {

    private final DataSource dataSource;

    // 스프링 컨테이너에 보관 중인 DataSource를 주입받습니다.
    public UserController(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * 신규 사용자 등록 (CREATE)
     * @param param 사용자 이름(name)과 이메일(email)을 담은 Map
     * @return 성공 또는 실패 메시지
     */
    /*
        fetch('http://localhost:8080/users', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: '테스트 사용자', email: 'test@test.com' })
        }).then(res => res.text()).then(console.log);
     */
    @PostMapping("/users")
    public String create(@RequestBody Map<String, String> param) {
        // 추가 쿼리 선인
        String sql = "INSERT INTO users (name, email) VALUES (?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, param.get("name"));
            preparedStatement.setString(2, param.get("email"));

            preparedStatement.executeUpdate();
            
            return "저장 성공!";
        } catch (Exception e) {
            e.printStackTrace();
            return "저장 실패: " + e.getMessage();
        }
    }

    /**
     * 사용자 단건 조회 (READ)
     * @param id 조회할 사용자의 고유 번호
     * @return 해당 사용자 정보 (없으면 null 또는 빈 Map)
     */
    /*
        http://localhost:8080/users/1
     */
    /*
        fetch('http://localhost:8080/users?id=1', {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        }).then(res => res.text()).then(console.log);
     */
    @GetMapping("/users/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        // 조회 쿼리 선언
        String sql = "SELECT id, name, email FROM users WHERE id = ?";
        Map<String, Object> user = new HashMap<>();

        // 자원 누수(Memory Leak) 방지를 위해 try-with-resources 사용
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setLong(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    user.put("id", resultSet.getLong("id"));
                    user.put("name", resultSet.getString("name"));
                    user.put("email", resultSet.getString("email"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return user;
    }

    /**
     * 사용자 목록 조회 (READ)
     * @param id    검색할 ID (선택사항)
     * @param name  검색할 이름 (선택사항, LIKE 검색)
     * @param email 검색할 이메일 (선택사항, LIKE 검색)
     * @return 조건에 맞는 사용자 정보(id, name, email) 리스트
     */
    /*
        http://localhost:8080/users?name=김
     */
    /*
        fetch('http://localhost:8080/users?name=김', {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        }).then(res => res.text()).then(console.log);
     */
    @GetMapping("/users")
    public List<Map<String, Object>> getList(@RequestParam(required = false) Long id, @RequestParam(required = false) String name, @RequestParam(required = false) String email) {
        // 목록 조회 쿼리 선인
        StringBuilder sql = new StringBuilder("SELECT id, name, email FROM users WHERE 1=1");

        // 조건 세팅
        if (id != null) {
            sql.append(" AND id = ?");
        }
        if (name != null && !name.isEmpty()) {
            sql.append(" AND name LIKE ?");
        }
        if (email != null && !email.isEmpty()) {
            sql.append(" AND email LIKE ?");
        }

        // 사용자 목록 조회 결과를 담을 리스트 선언
        List<Map<String, Object>> userList = new ArrayList<>();
        
        // 자원 누수(Memory Leak) 방지를 위해 try-with-resources 사용
        // 이 구문이 끝나면 connection, statement, resultSet이 자동으로 close() 됩니다.
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;

            if (id != null) {
                preparedStatement.setLong(paramIndex++, id);
            }
            if (name != null && !name.isEmpty()) {
                preparedStatement.setString(paramIndex++, "%" + name + "%");
            }
            if (email != null && !email.isEmpty()) {
                preparedStatement.setString(paramIndex++, "%" + email + "%");
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", resultSet.getLong("id"));
                    user.put("name", resultSet.getString("name"));
                    user.put("email", resultSet.getString("email"));

                    userList.add(user);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 스프링이 List<Map>을 JSON 배열로 자동 변환
        return userList;
    }

    /**
     * 기존 사용자 정보 수정 (UPDATE)
     * @param id 수정할 사용자의 고유 번호
     * @param param 수정할 이름(name)과 이메일(email)
     * @return 성공 또는 실패 메시지
     */
    /*
        fetch('http://localhost:8080/users/2', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: '김영희'})
        }).then(res => res.text()).then(console.log);
     */
    @PutMapping("/users/{id}")
    public String update(@PathVariable Long id, @RequestBody Map<String, String> param) {
        // 수정 쿼리 선인
        StringBuilder sql = new StringBuilder("UPDATE users SET id = ?");
        String name = param.get("name");
        String email = param.get("email");
        if (name != null && !name.isEmpty()) {
            sql.append(", name = ?");
        }
        if (email != null && !email.isEmpty()) {
            sql.append(", email = ?");
        }
        sql.append(" WHERE id = ?");

        // 자원 누수(Memory Leak) 방지를 위해 try-with-resources 사용
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;

            preparedStatement.setLong(paramIndex++, id);
            if (name != null && !name.isEmpty()) {
                preparedStatement.setString(paramIndex++, name);
            }
            if (email != null && !email.isEmpty()) {
                preparedStatement.setString(paramIndex++, email);
            }
            preparedStatement.setLong(paramIndex++, id);

            preparedStatement.executeUpdate();

            return "수정 성공!";
        } catch (Exception e) {
            e.printStackTrace();
            return "수정 실패";
        }
    }
    
    /**
     * 특정 사용자 삭제 (DELETE)
     * @param id 삭제할 사용자의 고유 번호
     * @return 성공 또는 실패 메시지
     */
    /*
        fetch('http://localhost:8080/users/4', {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json' }
        }).then(res => res.text()).then(console.log);
     */
    @DeleteMapping("/users/{id}")
    public String delete(@PathVariable Long id) {
        // 삭제 쿼리 선인
        String sql = "DELETE FROM users WHERE id = ?";

        // 자원 누수(Memory Leak) 방지를 위해 try-with-resources 사용
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, id);
            preparedStatement.executeUpdate();
            
            return "삭제 성공!";
        } catch (Exception e) {
            e.printStackTrace();
            return "삭제 실패";
        }
    }
}