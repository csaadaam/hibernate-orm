/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.function;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * @author Gavin King
 */
public abstract class AbstractSqmSelfRenderingFunctionDescriptor
		extends AbstractSqmFunctionDescriptor implements FunctionRenderingSupport {

	private final FunctionKind functionKind;

	public AbstractSqmSelfRenderingFunctionDescriptor(
			String name,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		super( name, argumentsValidator, returnTypeResolver );
		this.functionKind = FunctionKind.NORMAL;
	}

	public AbstractSqmSelfRenderingFunctionDescriptor(
			String name,
			FunctionKind functionKind,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		super( name, argumentsValidator, returnTypeResolver );
		this.functionKind = functionKind;
	}

	@Override
	public FunctionKind getFunctionKind() {
		return functionKind;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		switch ( functionKind ) {
			case ORDERED_SET_AGGREGATE:
				return generateOrderedSetAggregateSqmExpression(
						arguments,
						null,
						null,
						impliedResultType,
						queryEngine,
						typeConfiguration
				);
			case AGGREGATE:
				return generateAggregateSqmExpression(
						arguments,
						null,
						impliedResultType,
						queryEngine,
						typeConfiguration
				);
			default:
				return new SelfRenderingSqmFunction<>(
						this,
						(sqlAppender, sqlAstArguments, walker) -> render(sqlAppender, sqlAstArguments, walker),
						arguments,
						impliedResultType,
						getArgumentsValidator(),
						getReturnTypeResolver(),
						queryEngine.getCriteriaBuilder(),
						getName()
				);
	}
	}

	@Override
	public <T> SelfRenderingSqmAggregateFunction<T> generateSqmAggregateFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		if ( functionKind != FunctionKind.AGGREGATE ) {
			throw new UnsupportedOperationException( "The function " + getName() + " is not an aggregate function!" );
		}
		return new SelfRenderingSqmAggregateFunction<>(
				this,
				this,
				arguments,
				filter,
				impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	@Override
	public <T> SelfRenderingSqmAggregateFunction<T> generateSqmOrderedSetAggregateFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			SqmOrderByClause withinGroupClause,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		if ( functionKind != FunctionKind.ORDERED_SET_AGGREGATE ) {
			throw new UnsupportedOperationException( "The function " + getName() + " is not an ordered set-aggregate function!" );
		}
		return new SelfRenderingSqmOrderedSetAggregateFunction<>(
				this,
				this,
				arguments,
				filter,
				withinGroupClause,
				impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	/**
	 * Must be overridden by subclasses
	 */
	public abstract void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker);

	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, walker );
	}

	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, walker );
	}
}