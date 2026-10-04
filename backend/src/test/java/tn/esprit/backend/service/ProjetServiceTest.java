package tn.esprit.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend.entity.Projet;
import tn.esprit.backend.repository.ProjetRepository;
import tn.esprit.backend.service.impl.ProjetServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjetServiceTest {

    @Mock
    ProjetRepository projetRepository;

    @InjectMocks
    ProjetServiceImpl projetService;

    @Test
    void addProjet_shouldSaveAndReturnProjet() {
        Projet p = Projet.builder().sujet("Gestion RH").build();
        when(projetRepository.save(p)).thenReturn(p);

        Projet result = projetService.addProjet(p);

        assertNotNull(result);
        assertEquals("Gestion RH", result.getSujet());
        verify(projetRepository, times(1)).save(p);
    }

    @Test
    void updateProjet_shouldSaveAndReturnUpdatedProjet() {
        Projet p = Projet.builder().id(1L).sujet("Nouveau sujet").build();
        when(projetRepository.save(p)).thenReturn(p);

        Projet result = projetService.updateProjet(p);

        assertEquals(1L, result.getId());
        assertEquals("Nouveau sujet", result.getSujet());
        verify(projetRepository).save(p);
    }

    @Test
    void deleteProjet_shouldCallRepositoryDeleteById() {
        projetService.deleteProjet(1L);

        verify(projetRepository, times(1)).deleteById(1L);
    }

    @Test
    void getProjetById_shouldReturnProjetWhenFound() {
        Projet p = Projet.builder().id(1L).sujet("Gestion RH").build();
        when(projetRepository.findById(1L)).thenReturn(Optional.of(p));

        Projet result = projetService.getProjetById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getProjetById_shouldReturnNullWhenNotFound() {
        when(projetRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(projetService.getProjetById(99L));
    }

    @Test
    void getAllProjets_shouldReturnAllProjets() {
        when(projetRepository.findAll()).thenReturn(List.of(
                Projet.builder().id(1L).sujet("A").build(),
                Projet.builder().id(2L).sujet("B").build()));

        List<Projet> result = projetService.getAllProjets();

        assertEquals(2, result.size());
        verify(projetRepository).findAll();
    }
}
