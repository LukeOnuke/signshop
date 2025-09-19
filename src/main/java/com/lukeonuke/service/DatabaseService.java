package com.lukeonuke.service;

import com.lukeonuke.SignShop;
import com.lukeonuke.model.ShopModel;
import com.lukeonuke.model.TransactionModel;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import java.time.Instant;

/**
 * Interaction between the database and the program. Possibly to be cached in the future.
 * */
public class DatabaseService {
    private final SessionFactory sessionFactory;

    private static DatabaseService instance = null;
    public static DatabaseService getInstance(){
        if(instance == null) instance = new DatabaseService();
        return instance;
    }

    private DatabaseService() {
        Configuration configuration = new Configuration();
        final ConfigurationService cs = ConfigurationService.getInstance();

        // Set Hibernate properties programmatically
        configuration.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
        configuration.setProperty("hibernate.connection.url", cs.getDBUrl());
        configuration.setProperty("hibernate.connection.username", cs.getDBUser());
        configuration.setProperty("hibernate.connection.password", cs.getDBPassword());
        //configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");

        configuration.setProperty("hibernate.format_sql", "true");
        configuration.setProperty("hibernate.hbm2ddl.auto", "update");

        // Register your annotated entity classes
        configuration.addAnnotatedClass(ShopModel.class);
        configuration.addAnnotatedClass(TransactionModel.class);

        // Build service registry
        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties())
                .build();

        // Build session factory
        sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        SignShop.LOGGER.info("        Setup database session factory!");
    }

    private Session getSession(){
        return sessionFactory.openSession();
    }

    public ShopModel getShopById(int id) {
        try(Session session = getSession()){
            return session.find(ShopModel.class, id);
        }
    }

    public void insertTransaction(TransactionModel transaction) {
        try (Session session = getSession()) {
            Transaction tx = session.beginTransaction();
            session.persist(transaction);
            tx.commit();
        }
    }

    public void insertShop(ShopModel shop) {
        try (Session session = getSession()) {
            Transaction tx = session.beginTransaction();
            session.persist(shop);
            tx.commit();
        }
    }

    public ShopModel getShopByPosition(ShopPosition position) {
        return getShopByPosition(position.toString());
    }

    public ShopModel getShopByPosition(String position) {
        try(Session session = getSession()) {
            String hql = "from ShopModel s where s.position = :position AND s.deletedAt IS NULL";
            return session.createQuery(hql, ShopModel.class)
                    .setParameter("position", position)
                    .uniqueResult();
        }
    }

    public void softDeleteShopById(int id) {
        try (Session session = getSession()) {
            Transaction tx = session.beginTransaction();
            ShopModel shop = session.find(ShopModel.class, id);
            if (shop != null) {
                shop.setDeletedAt(Instant.now());
                session.merge(shop);
            }
            tx.commit();
        }
    }

    public void softDeleteShopByPosition(String position) {
        try (Session session = getSession()) {
            Transaction tx = session.beginTransaction();
            ShopModel shop = session.createQuery(
                            "from ShopModel s where s.position = :pos", ShopModel.class)
                    .setParameter("pos", position)
                    .uniqueResult();
            if (shop != null) {
                shop.setDeletedAt(Instant.now());
                session.merge(shop);
            }
            tx.commit();
        }
    }
}
