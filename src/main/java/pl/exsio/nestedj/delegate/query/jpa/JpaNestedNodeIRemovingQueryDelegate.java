package pl.exsio.nestedj.delegate.query.jpa;

import pl.exsio.nestedj.delegate.query.NestedNodeRemovingQueryDelegate;
import pl.exsio.nestedj.discriminator.TreeDiscriminator;
import pl.exsio.nestedj.ex.InvalidNodeException;
import pl.exsio.nestedj.model.NestedNode;
import pl.exsio.nestedj.model.NestedNodeInfo;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.Optional;

import static pl.exsio.nestedj.model.NestedNode.*;

public class JpaNestedNodeIRemovingQueryDelegate<ID extends Serializable, N extends NestedNode<ID>>
        extends JpaNestedNodeQueryDelegate<ID, N>
        implements NestedNodeRemovingQueryDelegate<ID, N> {

    private final static Long UPDATE_INCREMENT_BY = 2L;

    public JpaNestedNodeIRemovingQueryDelegate(EntityManager entityManager, TreeDiscriminator<ID, N> treeDiscriminator,
                                               Class<N> nodeClass, Class<ID> idClass) {
        super(entityManager, treeDiscriminator, nodeClass, idClass);
    }

    @Override
    public void updateNodesParent(NestedNodeInfo<ID, N> node) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<N> update = cb.createCriteriaUpdate(node.getNodeClass());
        Root<N> root = update.from(node.getNodeClass());
        update.set(root.get(PARENT_ID),  findNodeParentId(node).orElse(null))
                .where(getPredicates(cb, root,
                        cb.greaterThanOrEqualTo(root.get(LEFT), node.getLeft()),
                        cb.lessThanOrEqualTo(root.get(RIGHT), node.getRight()),
                        cb.equal(root.<Long>get(LEVEL), node.getLevel() + 1)
                ));
        entityManager.createQuery(update).executeUpdate();
    }

    @Override
    public void performSingleDeletion(NestedNodeInfo<ID, N> node) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<N> delete = cb.createCriteriaDelete(node.getNodeClass());
        Root<N> root = delete.from(node.getNodeClass());
        delete.where(getPredicates(cb, root,
                cb.equal(root.<Long>get(ID), node.getId())
        ));
        entityManager.createQuery(delete).executeUpdate();
    }

    private Optional<ID> findNodeParentId(NestedNodeInfo<ID, N> node) {
        if (node.getLevel() > 0) {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<ID> select = cb.createQuery(node.getIdClass());
            Root<N> root = select.from(node.getNodeClass());
            select.select(root.get(ID)).where(getPredicates(cb, root,
                    cb.lessThan(root.get(LEFT), node.getLeft()),
                    cb.greaterThan(root.get(RIGHT), node.getRight()),
                    cb.equal(root.<Long>get(LEVEL), node.getLevel() - 1)
            ));
            try {
                return Optional.of(entityManager.createQuery(select).setMaxResults(1).getSingleResult());
            } catch (NoResultException ex) {
                throw new InvalidNodeException(String.format("Couldn't find node's parent, although its level is greater than 0. It seems the tree is malformed: %s", node));
            }
        }
        return Optional.empty();
    }

    @Override
    public void updateSideFieldsBeforeSingleNodeRemoval(Long from, String field) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<N> update = cb.createCriteriaUpdate(nodeClass);
        Root<N> root = update.from(nodeClass);

        update.set(root.<Long>get(field), cb.diff(root.get(field), 2L))
                .where(getPredicates(cb, root, cb.greaterThan(root.get(field), from)));

        entityManager.createQuery(update).executeUpdate();
    }


    @Override
    public void updateDeletedNodesChildren(NestedNodeInfo<ID, N> node) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<N> update = cb.createCriteriaUpdate(node.getNodeClass());
        Root<N> root = update.from(node.getNodeClass());
        update.set(root.<Long>get(RIGHT), cb.diff(root.get(RIGHT), 1L))
                .set(root.<Long>get(LEFT), cb.diff(root.get(LEFT), 1L))
                .set(root.<Long>get(LEVEL), cb.diff(root.get(LEVEL), 1L));

        update.where(getPredicates(cb, root,
                cb.lessThan(root.get(RIGHT), node.getRight()),
                cb.greaterThan(root.get(LEFT), node.getLeft()))
        );

        entityManager.createQuery(update).executeUpdate();
    }

    @Override
    public void updateSideFieldsAfterSubtreeRemoval(Long from, Long delta, String field) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<N> update = cb.createCriteriaUpdate(nodeClass);
        Root<N> root = update.from(nodeClass);

        update.set(root.<Long>get(field), cb.diff(root.get(field), delta))
                .where(getPredicates(cb, root, cb.greaterThan(root.get(field), from)));

        entityManager.createQuery(update).executeUpdate();
    }

    @Override
    public void performBatchDeletion(NestedNodeInfo<ID, N> node) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<N> delete = cb.createCriteriaDelete(node.getNodeClass());
        Root<N> root = delete.from(node.getNodeClass());
        delete.where(getPredicates(cb, root,
                cb.greaterThanOrEqualTo(root.get(LEFT), node.getLeft()),
                cb.lessThanOrEqualTo(root.get(RIGHT), node.getRight())
        ));

        entityManager.createQuery(delete).executeUpdate();
    }
}
