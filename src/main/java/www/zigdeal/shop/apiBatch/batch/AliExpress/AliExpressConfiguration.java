package www.zigdeal.shop.apiBatch.batch.AliExpress;


import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemProcessor;

import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import www.zigdeal.shop.apiBatch.batch.Product;

import www.zigdeal.shop.apiBatch.service.PriceComparisonService;
import www.zigdeal.shop.apiBatch.service.TranslateService;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AliExpressConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final TranslateService translateService;
    private final PriceComparisonService priceComparisonService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Bean
    public Job AliExpressJob() {
        return jobBuilderFactory.get("AliExpressJob")
                .start(AliExpressStep())
                .build();
    }

    @Bean
    public Step AliExpressStep(){
        return stepBuilderFactory.get("AliExpressStep")
                .<Product, Product>chunk(10)
                .reader(AliExpressItemReader())
                .processor(compositeItemProcessor())
                .writer(productMongoItemWriter())
                .build();
    }

    @Bean
    public ItemReader<Product> AliExpressItemReader() {
        return new AliExpressReader();
    }

    public CompositeItemProcessor compositeItemProcessor() {
        List<ItemProcessor> delagates = new ArrayList<>();
        delagates.add(validateProcessor());
        delagates.add(translateProcessor());
        delagates.add(priceComparisonProcessor());

        CompositeItemProcessor processor = new CompositeItemProcessor<>();

        processor.setDelegates(delagates);

        return processor;
    }

    public ItemProcessor<Product, Product> validateProcessor() {
        return product -> {
            if (product.getPrice() < 0) return null;
            else return product;
        };
    }

    public ItemProcessor<Product, Product> translateProcessor() {
        return translateService::translateProduct;
    }


    public ItemProcessor<Product, Product> priceComparisonProcessor() {
        return priceComparisonService::comparePrice;
    }

    public MongoItemWriter<Product> productMongoItemWriter() {
        return new MongoItemWriterBuilder<Product>().template(mongoTemplate).collection("productBatchTest").build();
    }
}