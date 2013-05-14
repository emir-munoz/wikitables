package org.deri.exreta.dal.dbpedia.query;

import java.io.IOException;
import java.util.List;

import org.deri.exreta.dal.dbpedia.dto.Relation;
import org.deri.exreta.dal.dbpedia.dto.Resource;
import org.deri.exreta.dal.dbpedia.dto.TableCell;
import org.deri.exreta.dal.dbpedia.dto.TableRelation;
import org.deri.exreta.dal.dbpedia.dto.Triple;
import org.deri.exreta.dal.dbpedia.query.QueryBuilder.EntityType;

/**
 * Data Access Object Interface
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * @since 2013-03-27
 * 
 */
public interface DAOInterface
{
	Resource getResourceURI(String terms, EntityType type);

	List<TableRelation> getDBpediaMainRelations(List<TableCell> resourceList);

	List<TableRelation> getDBpediaRelationsCell(List<TableCell> resourceList);

	List<Relation> getRelations(Resource resource1, Resource resource2) throws IOException;

	boolean existsTriple(Triple triple);
}
