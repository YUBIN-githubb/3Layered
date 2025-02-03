package com.example.layerd.repository;


import com.example.layerd.dto.MemoResponseDto;
import com.example.layerd.entity.Memo;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcTemplateMemoRepository implements MemoRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTemplateMemoRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public MemoResponseDto saveMemo(Memo memo) {

        //insert query를 문자열로 직접 작성하지 않아도 됨
        SimpleJdbcInsert jdbcInsert = new SimpleJdbcInsert(jdbcTemplate);
        //memo라는 테이블 설정 + 이 테이블의 키값은 id
        jdbcInsert.withTableName("memo").usingGeneratedKeyColumns("id");

        Map<String, Object> params = new HashMap<>();
        params.put("title", memo.getTitle());
        params.put("contents", memo.getContents());

        //저장 후 생성된 key값을 Number 타입으로 반환
        Number key = jdbcInsert.executeAndReturnKey(new MapSqlParameterSource(params));

        return new MemoResponseDto(key.longValue(), memo.getTitle(), memo.getContents());
    }

    @Override
    public List<MemoResponseDto> findAllMemos() {
        return jdbcTemplate.query("select * from memo", memoRowMapper());
    }

    @Override
    public Optional<Memo> findMemoById(Long id) {

        List<Memo> result = jdbcTemplate.query("select * from memo where id=?", memoRowMapperV2(), id);


        return result.stream().findAny();
    }

    @Override
    public int updateMemo(Long id, String title, String contents) {
        //쿼리에 반연된 로우의 수가 int형으로 반환됨
        return jdbcTemplate.update("update memo set title = ?, contents = ? where id = ?", title, contents, id);

    }

    @Override
    public int updateTitle(Long id, String title) {
        return jdbcTemplate.update("update memo set title = ? where id = ?", title, id);
    }

    @Override
    public int deleteMemo(Long id) {
        return jdbcTemplate.update("delete from memo where id = ?", id);
    }

    @Override
    public Memo findMemoByIdOrElseThrow(Long id) {
        List<Memo> result = jdbcTemplate.query("select * from memo where id = ?", memoRowMapperV2(), id);
        return result.stream().findAny().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "id가 존재하지 않습니다."));
    }


    private RowMapper<MemoResponseDto> memoRowMapper () {
        return new RowMapper<MemoResponseDto>() {
            @Override
            public MemoResponseDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new MemoResponseDto(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("contents")
                );
            }
        };
    }

    private RowMapper<Memo> memoRowMapperV2 () {
        return new RowMapper<Memo>() {

            @Override
            public Memo mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new Memo(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("contents")
                );
            }
        };
    }
}
