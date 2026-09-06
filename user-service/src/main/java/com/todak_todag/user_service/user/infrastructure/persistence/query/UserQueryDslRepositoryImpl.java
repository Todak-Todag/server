package com.todak_todag.user_service.user.infrastructure.persistence.query;

import static com.todak_todag.user_service.user.domain.entity.QRegion.region;
import static com.todak_todag.user_service.user.domain.entity.user.QUser.user;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.todak_todag.user_service.user.application.query.UserSearchQuery;
import com.todak_todag.user_service.user.application.result.UserSearchResult;
import com.todak_todag.user_service.user.infrastructure.persistence.UserQueryDslRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserQueryDslRepositoryImpl implements UserQueryDslRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<UserSearchResult> search(UserSearchQuery query, Pageable pageable) {
		BooleanBuilder conditions = createConditions(query);
		
		List<UserSearchResult> result = queryFactory
				.select(
						Projections.constructor(UserSearchResult.class,
								user.id,
								user.name,
								user.phone,
								region.province,
								region.district,
								user.regionId,
								user.status,
								user.role,
								user.deletedAt.isNotNull())
				)
				.from(user)
				.leftJoin(region).on(user.regionId.eq(region.id))
				.where(conditions)
				.orderBy(user.createdAt.desc())
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch();
		
		JPAQuery<Long> count = queryFactory
				.select(user.count())
				.from(user)
				.where(conditions);
		
		Long total = count.fetchOne();
		
		return new PageImpl<UserSearchResult>(result, pageable, total != null ? total : 0L);
	}

	private BooleanBuilder createConditions(UserSearchQuery query) {
		BooleanBuilder builder = new BooleanBuilder();
		
		if(query.roles() != null && !query.roles().isEmpty()) {
			builder.and(user.role.in(query.roles()));
		}
		
		if(query.status() != null) {
			builder.and(user.status.eq(query.status()));
		}

		if(query.regionId() != null) {
			builder.and(user.regionId.eq(query.regionId()));
		}

		return builder;
	}
	
	
}
