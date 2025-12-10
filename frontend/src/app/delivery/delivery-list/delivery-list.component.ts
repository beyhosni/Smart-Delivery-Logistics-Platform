import { Component, OnInit } from '@angular/core';
import { DeliveryService } from '../../services/delivery.service';

@Component({
  selector: 'app-delivery-list',
  templateUrl: './delivery-list.component.html',
  styleUrls: ['./delivery-list.component.scss']
})
export class DeliveryListComponent implements OnInit {
  deliveries: any[] = [];
  loading = true;
  error = '';

  // Pagination
  page = 1;
  pageSize = 10;
  totalItems = 0;

  // Filtrage
  searchTerm = '';
  statusFilter = '';
  dateFilter = '';

  constructor(private deliveryService: DeliveryService) { }

  ngOnInit(): void {
    this.loadDeliveries();
  }

  loadDeliveries(): void {
    this.loading = true;

    // Construction des paramètres de requête
    let params = `?page=${this.page}&size=${this.pageSize}`;

    if (this.searchTerm) {
      params += `&search=${this.searchTerm}`;
    }

    if (this.statusFilter) {
      params += `&status=${this.statusFilter}`;
    }

    if (this.dateFilter) {
      params += `&date=${this.dateFilter}`;
    }

    this.deliveryService.getAllDeliveries().subscribe({
      next: (data) => {
        // Simulation de la pagination et du filtrage (côté client pour cet exemple)
        let filteredData = data;

        if (this.searchTerm) {
          filteredData = filteredData.filter(d => 
            d.id.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
            d.recipientName.toLowerCase().includes(this.searchTerm.toLowerCase())
          );
        }

        if (this.statusFilter) {
          filteredData = filteredData.filter(d => d.status === this.statusFilter);
        }

        if (this.dateFilter) {
          filteredData = filteredData.filter(d => 
            new Date(d.createdAt).toDateString() === new Date(this.dateFilter).toDateString()
          );
        }

        this.totalItems = filteredData.length;

        // Pagination
        const startIndex = (this.page - 1) * this.pageSize;
        this.deliveries = filteredData.slice(startIndex, startIndex + this.pageSize);

        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des livraisons';
        this.loading = false;
        console.error(err);
      }
    });
  }

  onPageChange(page: number): void {
    this.page = page;
    this.loadDeliveries();
  }

  onSearch(): void {
    this.page = 1; // Réinitialiser à la première page lors d'une nouvelle recherche
    this.loadDeliveries();
  }

  onFilterChange(): void {
    this.page = 1; // Réinitialiser à la première page lors d'un changement de filtre
    this.loadDeliveries();
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'CREATED':
        return 'bg-secondary';
      case 'ASSIGNED':
        return 'bg-info';
      case 'IN_TRANSIT':
        return 'bg-primary';
      case 'DELIVERED':
        return 'bg-success';
      case 'CANCELLED':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  }

  getStatusText(status: string): string {
    switch (status) {
      case 'CREATED':
        return 'Créée';
      case 'ASSIGNED':
        return 'Assignée';
      case 'IN_TRANSIT':
        return 'En transit';
      case 'DELIVERED':
        return 'Livrée';
      case 'CANCELLED':
        return 'Annulée';
      default:
        return status;
    }
  }

  getPriorityBadgeClass(priority: string): string {
    switch (priority) {
      case 'LOW':
        return 'bg-success';
      case 'NORMAL':
        return 'bg-info';
      case 'HIGH':
        return 'bg-warning';
      case 'URGENT':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  }

  getPriorityText(priority: string): string {
    switch (priority) {
      case 'LOW':
        return 'Basse';
      case 'NORMAL':
        return 'Normale';
      case 'HIGH':
        return 'Haute';
      case 'URGENT':
        return 'Urgente';
      default:
        return priority;
    }
  }
}
